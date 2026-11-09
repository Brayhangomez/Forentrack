package com.forentrack.web.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.forentrack.web.client.LaboratorioClient;
import com.forentrack.web.client.UsuariosClient;
import com.forentrack.web.dto.DirectorioDto;
import com.forentrack.web.dto.SolicitudDto;
import com.forentrack.web.model.Permiso;
import com.forentrack.web.model.Rol;
import com.forentrack.web.seguridad.RequierePermiso;
import com.forentrack.web.seguridad.SesionUsuario;

import jakarta.servlet.http.HttpSession;

@Controller
public class LaboratorioWebController extends ControladorBase {

    private static final List<String> ESTADOS = List.of("SOLICITADA", "ASIGNADA", "EN_PROCESO", "DICTAMINADA", "APROBADA");
    private static final String VOLVER = "/laboratorio";

    private final LaboratorioClient laboratorioClient;
    private final UsuariosClient usuariosClient;

    public LaboratorioWebController(LaboratorioClient laboratorioClient, UsuariosClient usuariosClient) {
        this.laboratorioClient = laboratorioClient;
        this.usuariosClient = usuariosClient;
    }

    @GetMapping("/laboratorio")
    @RequierePermiso(Permiso.LAB_VER)
    public String listar(@RequestParam(required = false) String estado,
                         @RequestParam(required = false) Boolean mias,
                         HttpSession sesion, Model model) {
        String token = token(sesion);
        Rol rol = SesionUsuario.rol(sesion);
        String correo = SesionUsuario.correo(sesion);
        boolean esAnalista = rol == Rol.PERITO || rol == Rol.ANALISTA_DIGITAL;
        // Peritos y analistas ven por defecto solo lo que tienen asignado
        boolean soloMias = esAnalista && (mias == null || mias);
        List<SolicitudDto> solicitudes = opcional(model,
                () -> laboratorioClient.listar(token, estado, soloMias ? correo : null, null), List.of());

        Map<String, Set<String>> acciones = new HashMap<>();
        solicitudes.forEach(s -> acciones.put(s.id(), acciones(s, rol, correo)));

        List<DirectorioDto> personal = Permiso.LAB_ASIGNAR.permite(rol)
                ? opcional(model, () -> usuariosClient.personalLaboratorio(token), List.of()) : List.of();
        model.addAttribute("peritos", personal.stream().filter(p -> "PERITO".equals(p.rol())).toList());
        model.addAttribute("analistas", personal.stream().filter(p -> "ANALISTA_DIGITAL".equals(p.rol())).toList());
        model.addAttribute("solicitudes", solicitudes);
        model.addAttribute("acc", acciones);
        model.addAttribute("estados", ESTADOS);
        model.addAttribute("estadoSel", estado);
        model.addAttribute("esAnalista", esAnalista);
        model.addAttribute("soloMias", soloMias);
        return "laboratorio";
    }

    /** Botones de cada solicitud según rol, estado, tipo y responsable asignado. */
    private static Set<String> acciones(SolicitudDto s, Rol rol, String correo) {
        Set<String> a = new HashSet<>();
        String e = s.estado();
        boolean mia = s.asignadoA() != null && s.asignadoA().equalsIgnoreCase(correo);
        boolean pericial = "PERICIAL".equals(s.tipoAnalisis());
        if (Permiso.LAB_ASIGNAR.permite(rol) && en(e, "SOLICITADA", "ASIGNADA")) {
            a.add(pericial ? "asignarPericial" : "asignarDigital");
        }
        if (Permiso.LAB_APROBAR.permite(rol) && "DICTAMINADA".equals(e)) {
            a.add("aprobar");
        }
        if (Permiso.LAB_RECHAZAR.permite(rol) && "DICTAMINADA".equals(e)) {
            a.add("rechazar");
        }
        if (mia && pericial && Permiso.LAB_PERITAJE_INICIAR.permite(rol) && "ASIGNADA".equals(e)) {
            a.add("iniciarPeritaje");
        }
        if (mia && pericial && Permiso.LAB_DICTAMEN.permite(rol) && "EN_PROCESO".equals(e)) {
            a.add("dictamen");
        }
        if (mia && !pericial && Permiso.LAB_DIGITAL_INICIAR.permite(rol) && "ASIGNADA".equals(e)) {
            a.add("iniciarDigital");
        }
        if (mia && !pericial && Permiso.LAB_HASH.permite(rol) && "EN_PROCESO".equals(e)) {
            a.add("hash");
        }
        if (mia && !pericial && Permiso.LAB_INFORME.permite(rol) && "EN_PROCESO".equals(e) && s.hashSha256() != null) {
            a.add("informe");
        }
        return a;
    }

    @PostMapping("/laboratorio/{id}/asignacion")
    @RequierePermiso(Permiso.LAB_ASIGNAR)
    public String asignar(@PathVariable String id, @RequestParam(required = false) String responsableCorreo,
                          HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, VOLVER, "Solicitud asignada",
                () -> laboratorioClient.asignar(token(sesion), id, responsableCorreo));
    }

    @PostMapping("/laboratorio/{id}/aprobacion")
    @RequierePermiso(Permiso.LAB_APROBAR)
    public String aprobar(@PathVariable String id, @RequestParam(required = false) String observacion,
                          HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, VOLVER, "Dictamen aprobado: la evidencia queda ANALIZADA",
                () -> laboratorioClient.aprobar(token(sesion), id, observacion));
    }

    @PostMapping("/laboratorio/{id}/rechazo")
    @RequierePermiso(Permiso.LAB_RECHAZAR)
    public String rechazar(@PathVariable String id, @RequestParam(required = false) String observacion,
                           HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, VOLVER, "Dictamen devuelto para corrección",
                () -> laboratorioClient.rechazar(token(sesion), id, observacion));
    }

    @PostMapping("/laboratorio/{id}/peritaje/inicio")
    @RequierePermiso(Permiso.LAB_PERITAJE_INICIAR)
    public String iniciarPeritaje(@PathVariable String id, HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, VOLVER, "Peritaje iniciado: la evidencia está EN_ANALISIS",
                () -> laboratorioClient.iniciarPeritaje(token(sesion), id));
    }

    @PostMapping("/laboratorio/{id}/peritaje/dictamen")
    @RequierePermiso(Permiso.LAB_DICTAMEN)
    public String dictamen(@PathVariable String id, @RequestParam(required = false) String hallazgos,
                           @RequestParam(required = false) String conclusiones,
                           HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, VOLVER, "Dictamen pericial emitido; pendiente de revisión del jefe de laboratorio",
                () -> laboratorioClient.dictamen(token(sesion), id, hallazgos, conclusiones));
    }

    @PostMapping("/laboratorio/{id}/digital/inicio")
    @RequierePermiso(Permiso.LAB_DIGITAL_INICIAR)
    public String iniciarDigital(@PathVariable String id, HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, VOLVER, "Análisis digital iniciado: la evidencia está EN_ANALISIS",
                () -> laboratorioClient.iniciarDigital(token(sesion), id));
    }

    @PostMapping("/laboratorio/{id}/digital/hash")
    @RequierePermiso(Permiso.LAB_HASH)
    public String hash(@PathVariable String id, @RequestParam(required = false) String hashSha256,
                       @RequestParam(required = false) String herramienta,
                       HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, VOLVER, "Hash SHA-256 registrado",
                () -> laboratorioClient.registrarHash(token(sesion), id, hashSha256, herramienta));
    }

    @PostMapping("/laboratorio/{id}/digital/informe")
    @RequierePermiso(Permiso.LAB_INFORME)
    public String informe(@PathVariable String id, @RequestParam(required = false) String hallazgos,
                          @RequestParam(required = false) String conclusiones,
                          HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, VOLVER, "Informe técnico digital emitido; pendiente de revisión",
                () -> laboratorioClient.informeDigital(token(sesion), id, hallazgos, conclusiones));
    }
}
