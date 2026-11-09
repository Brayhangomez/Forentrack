package com.forentrack.web.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.forentrack.web.client.ApiClienteException;
import com.forentrack.web.client.CasosClient;
import com.forentrack.web.client.CustodiaClient;
import com.forentrack.web.client.EvidenciasClient;
import com.forentrack.web.client.LaboratorioClient;
import com.forentrack.web.dto.CasoDto;
import com.forentrack.web.dto.EvidenciaDto;
import com.forentrack.web.dto.EvidenciaForm;
import com.forentrack.web.dto.MovimientoDto;
import com.forentrack.web.dto.SolicitudDto;
import com.forentrack.web.dto.ValidacionDto;
import com.forentrack.web.model.Permiso;
import com.forentrack.web.model.Rol;
import com.forentrack.web.seguridad.RequierePermiso;
import com.forentrack.web.seguridad.SesionUsuario;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/** Evidencias y las acciones de custodia, legales y de laboratorio que se hacen desde su detalle. */
@Controller
public class EvidenciaWebController extends ControladorBase {

    static final List<String> ESTADOS = List.of("REGISTRADA", "EN_CUSTODIA", "EN_ANALISIS", "ANALIZADA",
            "DEVUELTA", "DESCARTADA");
    private static final List<String> TIPOS = List.of("FISICA", "DIGITAL", "DOCUMENTAL");
    private static final List<String> FLUJO = List.of("REGISTRADA", "EN_CUSTODIA", "EN_ANALISIS", "ANALIZADA", "DEVUELTA");
    private static final String DETALLE = "/evidencias/{id}";

    /** Paso del indicador de progreso de la evidencia. */
    public record Paso(String estado, boolean hecho, boolean actual) {
    }

    private final EvidenciasClient evidenciasClient;
    private final CasosClient casosClient;
    private final CustodiaClient custodiaClient;
    private final LaboratorioClient laboratorioClient;

    public EvidenciaWebController(EvidenciasClient evidenciasClient, CasosClient casosClient,
                                  CustodiaClient custodiaClient, LaboratorioClient laboratorioClient) {
        this.evidenciasClient = evidenciasClient;
        this.casosClient = casosClient;
        this.custodiaClient = custodiaClient;
        this.laboratorioClient = laboratorioClient;
    }

    @ModelAttribute("tipos")
    public List<String> tipos() {
        return TIPOS;
    }

    @GetMapping("/evidencias")
    @RequierePermiso(Permiso.EVIDENCIA_VER)
    public String listar(@RequestParam(required = false) String casoId,
                         @RequestParam(required = false) String estado,
                         @RequestParam(required = false) String tipo,
                         HttpSession sesion, Model model) {
        String token = token(sesion);
        model.addAttribute("evidencias", opcional(model, () -> evidenciasClient.listar(token, casoId, estado, tipo), List.of()));
        model.addAttribute("casos", opcional(model, () -> casosClient.buscar(token, null, null, null), List.of()));
        model.addAttribute("casoSel", casoId);
        model.addAttribute("estadoSel", estado);
        model.addAttribute("tipoSel", tipo);
        model.addAttribute("estados", ESTADOS);
        return "evidencias";
    }

    @GetMapping("/evidencias/nuevo")
    @RequierePermiso(Permiso.EVIDENCIA_REGISTRAR)
    public String formulario(@RequestParam(required = false) String casoId, HttpSession sesion, Model model) {
        EvidenciaForm form = new EvidenciaForm();
        form.setCasoId(casoId);
        model.addAttribute("form", form);
        cargarCasosActivos(token(sesion), model);
        return "evidencia-form";
    }

    @PostMapping("/evidencias")
    @RequierePermiso(Permiso.EVIDENCIA_REGISTRAR)
    public String crear(@Valid @ModelAttribute("form") EvidenciaForm form, BindingResult resultado,
                        HttpSession sesion, Model model, RedirectAttributes flash) {
        String token = token(sesion);
        if (resultado.hasErrors()) {
            cargarCasosActivos(token, model);
            return "evidencia-form";
        }
        try {
            EvidenciaDto creada = evidenciasClient.crear(token, form);
            flash.addFlashAttribute("ok", "Evidencia registrada con código " + creada.codigo()
                    + ". Completa el acta de recolección para poder entregarla a custodia.");
            return "redirect:/evidencias/" + creada.id();
        } catch (ApiClienteException ex) {
            if (!ex.esNegocio()) {
                throw ex;
            }
            mostrarErrores(ex, resultado, model);
            cargarCasosActivos(token, model);
            return "evidencia-form";
        }
    }

    @GetMapping("/evidencias/{id}")
    @RequierePermiso(Permiso.EVIDENCIA_VER)
    public String detalle(@PathVariable String id, HttpSession sesion, Model model) {
        String token = token(sesion);
        Rol rol = SesionUsuario.rol(sesion);
        EvidenciaDto ev = evidenciasClient.obtener(token, id);
        List<MovimientoDto> movimientos = Permiso.CUSTODIA_VER.permite(rol)
                ? opcional(model, () -> custodiaClient.historial(token, id), List.of()) : List.of();
        List<ValidacionDto> validaciones = Permiso.CUSTODIA_VER.permite(rol)
                ? opcional(model, () -> custodiaClient.validaciones(token, id), List.of()) : List.of();
        List<SolicitudDto> solicitudes = Permiso.LAB_VER.permite(rol)
                ? opcional(model, () -> laboratorioClient.listar(token, null, null, id), List.of()) : List.of();

        boolean entregada = movimientos.stream().anyMatch(m -> "ENTREGA".equals(m.tipo()));
        boolean recibida = movimientos.stream().anyMatch(m -> "RECEPCION".equals(m.tipo()));
        String ultimaValidacion = validaciones.isEmpty() ? null : validaciones.get(0).resultado();
        boolean solicitudActiva = solicitudes.stream().anyMatch(s -> !"APROBADA".equals(s.estado()));
        String e = ev.estadoActual();
        boolean acta = Boolean.TRUE.equals(ev.actaCompleta());

        Map<String, Boolean> a = new LinkedHashMap<>();
        a.put("acta", Permiso.EVIDENCIA_ACTA.permite(rol) && "REGISTRADA".equals(e));
        a.put("entregar", Permiso.CUSTODIA_ENTREGAR.permite(rol) && "REGISTRADA".equals(e) && acta && !entregada);
        a.put("recibir", Permiso.CUSTODIA_RECIBIR.permite(rol) && "REGISTRADA".equals(e) && entregada);
        a.put("trasladar", Permiso.CUSTODIA_TRASLADAR.permite(rol) && en(e, "EN_CUSTODIA", "EN_ANALISIS", "ANALIZADA"));
        a.put("devolver", Permiso.CUSTODIA_DEVOLVER.permite(rol) && "ANALIZADA".equals(e));
        a.put("validar", Permiso.CUSTODIA_VALIDAR.permite(rol) && recibida);
        a.put("descartar", Permiso.EVIDENCIA_DESCARTAR.permite(rol) && en(e, "REGISTRADA", "EN_CUSTODIA", "ANALIZADA"));
        a.put("solicitar", Permiso.LAB_SOLICITAR.permite(rol) && "EN_CUSTODIA".equals(e) && !solicitudActiva);

        model.addAttribute("ev", ev);
        model.addAttribute("acciones", a);
        model.addAttribute("hayAcciones", a.containsValue(Boolean.TRUE));
        model.addAttribute("devolucionHabilitada", "VALIDA".equals(ultimaValidacion));
        model.addAttribute("pendienteEntrega", "REGISTRADA".equals(e) && !entregada);
        model.addAttribute("movimientos", movimientos);
        model.addAttribute("validaciones", validaciones);
        model.addAttribute("solicitudes", solicitudes);
        model.addAttribute("pasos", pasos(e));
        return "evidencia-detalle";
    }

    private static List<Paso> pasos(String actual) {
        int indice = FLUJO.indexOf(actual);
        List<Paso> pasos = new ArrayList<>();
        for (int i = 0; i < FLUJO.size(); i++) {
            pasos.add(new Paso(FLUJO.get(i), indice >= 0 && i <= indice, i == indice));
        }
        return pasos;
    }

    @PostMapping("/evidencias/{id}/acta")
    @RequierePermiso(Permiso.EVIDENCIA_ACTA)
    public String acta(@PathVariable String id, @RequestParam(required = false) String lugarHallazgo,
                       @RequestParam(required = false) String fechaHallazgo,
                       @RequestParam(required = false) String embalaje,
                       @RequestParam(required = false) String observaciones,
                       HttpSession sesion, RedirectAttributes flash) {
        String fecha = fechaHallazgo == null || fechaHallazgo.isBlank() ? null : fechaHallazgo;
        return accion(flash, DETALLE, "Acta de recolección guardada",
                () -> evidenciasClient.completarActa(token(sesion), id, lugarHallazgo, fecha, embalaje, observaciones));
    }

    @PostMapping("/evidencias/{id}/entrega")
    @RequierePermiso(Permiso.CUSTODIA_ENTREGAR)
    public String entregar(@PathVariable String id, @RequestParam(required = false) String destino,
                           @RequestParam(required = false) String observacion,
                           HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, DETALLE, "Evidencia entregada a custodia; queda pendiente de recepción",
                () -> custodiaClient.entregar(token(sesion), id, destino, observacion));
    }

    @PostMapping("/evidencias/{id}/recepcion")
    @RequierePermiso(Permiso.CUSTODIA_RECIBIR)
    public String recibir(@PathVariable String id, @RequestParam(required = false) String ubicacion,
                          @RequestParam(required = false) String observacion,
                          HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, DETALLE, "Evidencia recibida en bodega (EN_CUSTODIA)",
                () -> custodiaClient.recibir(token(sesion), id, ubicacion, observacion));
    }

    @PostMapping("/evidencias/{id}/traslado")
    @RequierePermiso(Permiso.CUSTODIA_TRASLADAR)
    public String trasladar(@PathVariable String id, @RequestParam(required = false) String destino,
                            @RequestParam(required = false) String motivo,
                            HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, DETALLE, "Traslado registrado en la cadena de custodia",
                () -> custodiaClient.trasladar(token(sesion), id, destino, motivo));
    }

    @PostMapping("/evidencias/{id}/devolucion")
    @RequierePermiso(Permiso.CUSTODIA_DEVOLVER)
    public String devolver(@PathVariable String id, @RequestParam(required = false) String destinatario,
                           @RequestParam(required = false) String observacion,
                           HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, DETALLE, "Evidencia devuelta",
                () -> custodiaClient.devolver(token(sesion), id, destinatario, observacion));
    }

    @PostMapping("/evidencias/{id}/validacion")
    @RequierePermiso(Permiso.CUSTODIA_VALIDAR)
    public String validar(@PathVariable String id, @RequestParam(required = false) String resultado,
                          @RequestParam(required = false) String observacion,
                          HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, DETALLE, "Validación de la cadena de custodia registrada",
                () -> custodiaClient.validar(token(sesion), id, resultado, observacion));
    }

    @PostMapping("/evidencias/{id}/descarte")
    @RequierePermiso(Permiso.EVIDENCIA_DESCARTAR)
    public String descartar(@PathVariable String id, @RequestParam(required = false) String motivo,
                            HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, DETALLE, "Descarte autorizado", () -> evidenciasClient.descartar(token(sesion), id, motivo));
    }

    @PostMapping("/evidencias/{id}/solicitud-analisis")
    @RequierePermiso(Permiso.LAB_SOLICITAR)
    public String solicitar(@PathVariable String id, @RequestParam(required = false) String objetivo,
                            @RequestParam(required = false) String prioridad,
                            HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, DETALLE, "Solicitud de análisis enviada al laboratorio",
                () -> laboratorioClient.solicitar(token(sesion), id, objetivo, prioridad));
    }

    /** Solo los casos que todavía admiten evidencias. */
    private void cargarCasosActivos(String token, Model model) {
        List<CasoDto> casos = opcional(model, () -> casosClient.buscar(token, null, null, null), List.<CasoDto>of());
        model.addAttribute("casos", casos.stream()
                .filter(c -> en(c.estado(), "ABIERTO", "ASIGNADO", "EN_INVESTIGACION")).toList());
    }
}
