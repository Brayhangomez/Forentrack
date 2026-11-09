package com.forentrack.web.controller;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.forentrack.web.client.AuditoriaClient;
import com.forentrack.web.dto.ResumenDto;
import com.forentrack.web.model.Permiso;
import com.forentrack.web.seguridad.RequierePermiso;

import jakarta.servlet.http.HttpSession;

/** Bitácora de auditoría: exclusiva del AUDITOR. */
@Controller
public class AuditoriaWebController extends ControladorBase {

    private static final List<String> SERVICIOS = List.of("usuarios-service", "casos-service", "evidencias-service",
            "cadena-custodia-service", "laboratorio-service", "auditoria-service");
    private static final List<String> RESULTADOS = List.of("EXITO", "FALLIDO", "DENEGADO");

    private final AuditoriaClient auditoriaClient;

    public AuditoriaWebController(AuditoriaClient auditoriaClient) {
        this.auditoriaClient = auditoriaClient;
    }

    @GetMapping("/auditoria")
    @RequierePermiso(Permiso.AUDITORIA_VER)
    public String listar(@RequestParam Map<String, String> parametros, HttpSession sesion, Model model) {
        String token = token(sesion);
        Map<String, String> filtros = filtros(parametros);
        Map<String, Object> consulta = new LinkedHashMap<>(filtros);
        consulta.put("limite", "300");
        model.addAttribute("eventos", opcional(model, () -> auditoriaClient.buscar(token, consulta), List.of()));
        model.addAttribute("resumen", opcional(model, () -> auditoriaClient.resumen(token),
                new ResumenDto(0L, 0L, 0L, 0L, 0L, Map.of(), Map.of())));
        model.addAttribute("f", filtros);
        model.addAttribute("servicios", SERVICIOS);
        model.addAttribute("resultados", RESULTADOS);
        UriComponentsBuilder exportar = UriComponentsBuilder.fromPath("/auditoria/exportar");
        filtros.forEach(exportar::queryParam);
        model.addAttribute("urlExportar", exportar.build().encode().toUriString());
        return "auditoria";
    }

    @GetMapping("/auditoria/exportar")
    @RequierePermiso(Permiso.AUDITORIA_EXPORTAR)
    public ResponseEntity<byte[]> exportar(@RequestParam Map<String, String> parametros, HttpSession sesion) {
        byte[] csv = auditoriaClient.exportar(token(sesion), filtros(parametros));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bitacora-forentrack.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv != null ? csv : new byte[0]);
    }

    @PostMapping("/auditoria/{id}/revision")
    @RequierePermiso(Permiso.AUDITORIA_REVISAR)
    public String revisar(@PathVariable String id, @RequestParam(required = false) String observacion,
                          HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, "/auditoria", "Revisión registrada",
                () -> auditoriaClient.revisar(token(sesion), id, observacion));
    }

    /** Solo se aceptan los filtros conocidos y con valor. */
    private static Map<String, String> filtros(Map<String, String> parametros) {
        Map<String, String> f = new LinkedHashMap<>();
        for (String clave : List.of("usuario", "accion", "servicio", "resultado", "desde", "hasta", "revisado")) {
            String valor = parametros.get(clave);
            if (valor != null && !valor.isBlank()) {
                f.put(clave, valor.trim());
            }
        }
        return f;
    }
}
