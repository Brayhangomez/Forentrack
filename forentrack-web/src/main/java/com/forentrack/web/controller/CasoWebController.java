package com.forentrack.web.controller;

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
import com.forentrack.web.client.EvidenciasClient;
import com.forentrack.web.client.UsuariosClient;
import com.forentrack.web.dto.CasoDto;
import com.forentrack.web.dto.CasoForm;
import com.forentrack.web.model.Permiso;
import com.forentrack.web.model.Rol;
import com.forentrack.web.seguridad.RequierePermiso;
import com.forentrack.web.seguridad.SesionUsuario;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class CasoWebController extends ControladorBase {

    static final List<String> ESTADOS = List.of("ABIERTO", "ASIGNADO", "EN_INVESTIGACION", "SUSPENDIDO", "CERRADO");
    private static final String DETALLE = "/casos/{id}";

    private final CasosClient casosClient;
    private final EvidenciasClient evidenciasClient;
    private final UsuariosClient usuariosClient;

    public CasoWebController(CasosClient casosClient, EvidenciasClient evidenciasClient, UsuariosClient usuariosClient) {
        this.casosClient = casosClient;
        this.evidenciasClient = evidenciasClient;
        this.usuariosClient = usuariosClient;
    }

    @GetMapping("/casos")
    @RequierePermiso(Permiso.CASO_VER)
    public String listar(@RequestParam(required = false) String codigo,
                         @RequestParam(required = false) String estado,
                         @RequestParam(defaultValue = "false") boolean mios,
                         HttpSession sesion, Model model) {
        String investigador = mios ? SesionUsuario.correo(sesion) : null;
        model.addAttribute("casos", opcional(model, () -> casosClient.buscar(token(sesion), codigo, estado, investigador), List.of()));
        model.addAttribute("codigo", codigo);
        model.addAttribute("estadoSel", estado);
        model.addAttribute("mios", mios);
        model.addAttribute("estados", ESTADOS);
        return "casos";
    }

    @GetMapping("/casos/nuevo")
    @RequierePermiso(Permiso.CASO_CREAR)
    public String formulario(Model model) {
        model.addAttribute("form", new CasoForm());
        return "caso-form";
    }

    @PostMapping("/casos")
    @RequierePermiso(Permiso.CASO_CREAR)
    public String crear(@Valid @ModelAttribute("form") CasoForm form, BindingResult resultado,
                        HttpSession sesion, Model model, RedirectAttributes flash) {
        if (resultado.hasErrors()) {
            return "caso-form";
        }
        try {
            CasoDto creado = casosClient.crear(token(sesion), form);
            flash.addFlashAttribute("ok", "Caso " + creado.codigo() + " registrado. Queda ABIERTO hasta que el fiscal lo asigne.");
            return "redirect:/casos/" + creado.id();
        } catch (ApiClienteException ex) {
            if (!ex.esNegocio()) {
                throw ex;
            }
            mostrarErrores(ex, resultado, model);
            return "caso-form";
        }
    }

    @GetMapping("/casos/{id}")
    @RequierePermiso(Permiso.CASO_VER)
    public String detalle(@PathVariable String id, HttpSession sesion, Model model) {
        String token = token(sesion);
        Rol rol = SesionUsuario.rol(sesion);
        CasoDto caso = casosClient.obtener(token, id);
        Map<String, Boolean> acciones = acciones(caso, rol, SesionUsuario.correo(sesion));
        model.addAttribute("caso", caso);
        model.addAttribute("acciones", acciones);
        model.addAttribute("hayAcciones", acciones.entrySet().stream()
                .anyMatch(e -> !e.getKey().equals("cierreHabilitado") && e.getValue()));
        model.addAttribute("evidencias", opcional(model, () -> evidenciasClient.listar(token, id, null, null), List.of()));
        model.addAttribute("investigadores", acciones.get("asignar")
                ? opcional(model, () -> usuariosClient.investigadores(token), List.of()) : List.of());
        return "caso-detalle";
    }

    /** Qué botones ve el rol según el estado del caso (el backend vuelve a validar todo). */
    private static Map<String, Boolean> acciones(CasoDto caso, Rol rol, String correo) {
        String e = caso.estado();
        boolean asignadoAMi = caso.investigadorAsignado() == null || caso.investigadorAsignado().equalsIgnoreCase(correo);
        Map<String, Boolean> a = new LinkedHashMap<>();
        a.put("asignar", Permiso.CASO_ASIGNAR.permite(rol) && en(e, "ABIERTO", "ASIGNADO"));
        a.put("iniciar", Permiso.CASO_INICIAR.permite(rol) && "ASIGNADO".equals(e) && asignadoAMi);
        a.put("nota", Permiso.CASO_NOTA.permite(rol) && "EN_INVESTIGACION".equals(e) && asignadoAMi);
        a.put("suspender", Permiso.CASO_SUSPENDER.permite(rol) && en(e, "ASIGNADO", "EN_INVESTIGACION"));
        a.put("reactivar", Permiso.CASO_REACTIVAR.permite(rol) && "SUSPENDIDO".equals(e));
        a.put("cerrar", Permiso.CASO_CERRAR.permite(rol) && "EN_INVESTIGACION".equals(e));
        a.put("cierreHabilitado", "FAVORABLE".equals(caso.conceptoLegal()));
        a.put("concepto", Permiso.CASO_CONCEPTO.permite(rol) && en(e, "EN_INVESTIGACION", "SUSPENDIDO"));
        a.put("registrarEvidencia", Permiso.EVIDENCIA_REGISTRAR.permite(rol) && en(e, "ABIERTO", "ASIGNADO", "EN_INVESTIGACION"));
        return a;
    }

    @PostMapping("/casos/{id}/asignacion")
    @RequierePermiso(Permiso.CASO_ASIGNAR)
    public String asignar(@PathVariable String id, @RequestParam(required = false) String investigadorCorreo,
                          HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, DETALLE, "Investigador asignado al caso",
                () -> casosClient.asignar(token(sesion), id, investigadorCorreo));
    }

    @PostMapping("/casos/{id}/inicio")
    @RequierePermiso(Permiso.CASO_INICIAR)
    public String iniciar(@PathVariable String id, HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, DETALLE, "Investigación iniciada", () -> casosClient.iniciar(token(sesion), id));
    }

    @PostMapping("/casos/{id}/notas")
    @RequierePermiso(Permiso.CASO_NOTA)
    public String nota(@PathVariable String id, @RequestParam(required = false) String texto,
                       HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, DETALLE, "Nota agregada", () -> casosClient.agregarNota(token(sesion), id, texto));
    }

    @PostMapping("/casos/{id}/suspension")
    @RequierePermiso(Permiso.CASO_SUSPENDER)
    public String suspender(@PathVariable String id, @RequestParam(required = false) String motivo,
                            HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, DETALLE, "Caso suspendido", () -> casosClient.suspender(token(sesion), id, motivo));
    }

    @PostMapping("/casos/{id}/reactivacion")
    @RequierePermiso(Permiso.CASO_REACTIVAR)
    public String reactivar(@PathVariable String id, @RequestParam(required = false) String observacion,
                            HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, DETALLE, "Caso reactivado", () -> casosClient.reactivar(token(sesion), id, observacion));
    }

    @PostMapping("/casos/{id}/concepto")
    @RequierePermiso(Permiso.CASO_CONCEPTO)
    public String concepto(@PathVariable String id, @RequestParam(required = false) String concepto,
                           @RequestParam(required = false) String fundamento,
                           HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, DETALLE, "Concepto legal registrado",
                () -> casosClient.emitirConcepto(token(sesion), id, concepto, fundamento));
    }

    @PostMapping("/casos/{id}/cierre")
    @RequierePermiso(Permiso.CASO_CERRAR)
    public String cerrar(@PathVariable String id, @RequestParam(required = false) String observacion,
                         HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, DETALLE, "Caso cerrado", () -> casosClient.cerrar(token(sesion), id, observacion));
    }
}
