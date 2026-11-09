package com.forentrack.auditoria.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.forentrack.auditoria.dto.EventoRequest;
import com.forentrack.auditoria.dto.FiltroEventos;
import com.forentrack.auditoria.dto.ResumenResponse;
import com.forentrack.auditoria.dto.RevisionRequest;
import com.forentrack.auditoria.model.EventoAuditoria;
import com.forentrack.auditoria.service.BitacoraService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auditoria")
@Tag(name = "Auditoría", description = "Bitácora de acciones: registro interno y consulta exclusiva del AUDITOR")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Datos inválidos"),
    @ApiResponse(responseCode = "401", description = "No autenticado o token expirado"),
    @ApiResponse(responseCode = "403", description = "Solo AUDITOR (o servicios internos para registrar)")
})
public class AuditoriaController {

    private final BitacoraService bitacora;

    public AuditoriaController(BitacoraService bitacora) {
        this.bitacora = bitacora;
    }

    @PostMapping("/eventos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar evento (uso interno entre servicios)",
               description = "Requiere la cabecera X-Internal-Key derivada de JWT_SECRET; el JWT del usuario es opcional.")
    public EventoAuditoria registrar(@Valid @RequestBody EventoRequest request,
                                     @Parameter(hidden = true)
                                     @RequestHeader(value = "X-Internal-Key", required = false) String clave) {
        return bitacora.recibir(request, clave);
    }

    @GetMapping("/eventos")
    @PreAuthorize("hasRole('AUDITOR')")
    @Operation(summary = "Consultar la bitácora con filtros", description = "Rol exclusivo: AUDITOR")
    public List<EventoAuditoria> buscar(@RequestParam(required = false) String usuario,
                                        @RequestParam(required = false) String accion,
                                        @RequestParam(required = false) String servicio,
                                        @RequestParam(required = false) String resultado,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                        @RequestParam(required = false) Boolean revisado,
                                        @RequestParam(required = false) Integer limite) {
        return bitacora.buscar(new FiltroEventos(usuario, accion, servicio, resultado, desde, hasta, revisado), limite);
    }

    @GetMapping("/eventos/exportar")
    @PreAuthorize("hasRole('AUDITOR')")
    @Operation(summary = "Exportar la bitácora en CSV", description = "Rol exclusivo: AUDITOR")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) String usuario,
                                           @RequestParam(required = false) String accion,
                                           @RequestParam(required = false) String servicio,
                                           @RequestParam(required = false) String resultado,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                           @RequestParam(required = false) Boolean revisado) {
        byte[] csv = bitacora.exportarCsv(new FiltroEventos(usuario, accion, servicio, resultado, desde, hasta, revisado));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bitacora-forentrack.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }

    @PostMapping("/eventos/{id}/revision")
    @PreAuthorize("hasRole('AUDITOR')")
    @Operation(summary = "Registrar revisión o hallazgo sobre un evento", description = "Rol exclusivo: AUDITOR")
    public EventoAuditoria revisar(@PathVariable String id, @Valid @RequestBody RevisionRequest request) {
        return bitacora.revisar(id, request.observacion());
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('AUDITOR')")
    @Operation(summary = "Indicadores de la bitácora", description = "Rol exclusivo: AUDITOR")
    public ResumenResponse resumen() {
        return bitacora.resumen();
    }
}
