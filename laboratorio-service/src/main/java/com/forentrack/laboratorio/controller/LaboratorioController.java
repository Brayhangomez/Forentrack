package com.forentrack.laboratorio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.forentrack.laboratorio.dto.AsignacionRequest;
import com.forentrack.laboratorio.dto.DictamenRequest;
import com.forentrack.laboratorio.dto.HashRequest;
import com.forentrack.laboratorio.dto.RevisionRequest;
import com.forentrack.laboratorio.dto.SolicitudRequest;
import com.forentrack.laboratorio.exception.ErrorResponse;
import com.forentrack.laboratorio.model.EstadoSolicitud;
import com.forentrack.laboratorio.model.SolicitudAnalisis;
import com.forentrack.laboratorio.model.TipoAnalisis;
import com.forentrack.laboratorio.service.LaboratorioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/laboratorio/solicitudes")
@Tag(name = "Laboratorio", description = "Solicitudes de análisis, peritaje, análisis digital y revisión de dictámenes")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado o token expirado"),
    @ApiResponse(responseCode = "403", description = "El rol no tiene permiso o no es el responsable asignado"),
    @ApiResponse(responseCode = "404", description = "Solicitud o evidencia no encontrada"),
    @ApiResponse(responseCode = "409", description = "El estado actual no permite la acción"),
    @ApiResponse(responseCode = "503", description = "Servicio dependiente no disponible")
})
public class LaboratorioController {

    private final LaboratorioService servicio;

    public LaboratorioController(LaboratorioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INVESTIGADOR')")
    @Operation(summary = "Solicitar análisis de una evidencia EN_CUSTODIA", description = "Rol exclusivo: INVESTIGADOR")
    public SolicitudAnalisis solicitar(@Valid @RequestBody SolicitudRequest request) {
        return servicio.solicitar(request);
    }

    @GetMapping
    @Operation(summary = "Listar solicitudes por estado, responsable y/o evidencia")
    public List<SolicitudAnalisis> listar(@RequestParam(required = false) EstadoSolicitud estado,
                                          @RequestParam(required = false) String asignadoA,
                                          @RequestParam(required = false) String evidenciaId) {
        return servicio.listar(estado, asignadoA, evidenciaId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una solicitud")
    public SolicitudAnalisis obtener(@PathVariable String id) {
        return servicio.obtener(id);
    }

    @PostMapping("/{id}/asignacion")
    @PreAuthorize("hasRole('JEFE_LABORATORIO')")
    @Operation(summary = "Asignar a perito o analista digital", description = "Rol exclusivo: JEFE_LABORATORIO")
    public SolicitudAnalisis asignar(@PathVariable String id, @Valid @RequestBody AsignacionRequest request) {
        return servicio.asignar(id, request.responsableCorreo());
    }

    @PostMapping("/{id}/aprobacion")
    @PreAuthorize("hasRole('JEFE_LABORATORIO')")
    @Operation(summary = "Aprobar dictamen (evidencia → ANALIZADA)", description = "Rol exclusivo: JEFE_LABORATORIO")
    public SolicitudAnalisis aprobar(@PathVariable String id,
                                     @Valid @RequestBody(required = false) RevisionRequest request) {
        return servicio.aprobar(id, request != null ? request.observacion() : null);
    }

    @PostMapping("/{id}/rechazo")
    @PreAuthorize("hasRole('JEFE_LABORATORIO')")
    @Operation(summary = "Devolver dictamen para corrección", description = "Rol exclusivo: JEFE_LABORATORIO")
    public SolicitudAnalisis rechazar(@PathVariable String id, @Valid @RequestBody RevisionRequest request) {
        return servicio.rechazar(id, request.observacion());
    }

    @PostMapping("/{id}/peritaje/inicio")
    @PreAuthorize("hasRole('PERITO')")
    @Operation(summary = "Iniciar peritaje físico/documental (evidencia → EN_ANALISIS)", description = "Rol exclusivo: PERITO")
    public SolicitudAnalisis iniciarPeritaje(@PathVariable String id) {
        return servicio.iniciar(id, TipoAnalisis.PERICIAL);
    }

    @PostMapping("/{id}/peritaje/dictamen")
    @PreAuthorize("hasRole('PERITO')")
    @Operation(summary = "Emitir dictamen pericial", description = "Rol exclusivo: PERITO")
    public SolicitudAnalisis dictamen(@PathVariable String id, @Valid @RequestBody DictamenRequest request) {
        return servicio.dictaminar(id, TipoAnalisis.PERICIAL, request);
    }

    @PostMapping("/{id}/digital/inicio")
    @PreAuthorize("hasRole('ANALISTA_DIGITAL')")
    @Operation(summary = "Iniciar análisis digital (evidencia → EN_ANALISIS)", description = "Rol exclusivo: ANALISTA_DIGITAL")
    public SolicitudAnalisis iniciarDigital(@PathVariable String id) {
        return servicio.iniciar(id, TipoAnalisis.DIGITAL);
    }

    @PostMapping("/{id}/digital/hash")
    @PreAuthorize("hasRole('ANALISTA_DIGITAL')")
    @Operation(summary = "Registrar hash SHA-256 de la imagen forense", description = "Rol exclusivo: ANALISTA_DIGITAL")
    public SolicitudAnalisis hash(@PathVariable String id, @Valid @RequestBody HashRequest request) {
        return servicio.registrarHash(id, request);
    }

    @PostMapping("/{id}/digital/informe")
    @PreAuthorize("hasRole('ANALISTA_DIGITAL')")
    @Operation(summary = "Emitir informe técnico digital (requiere hash)", description = "Rol exclusivo: ANALISTA_DIGITAL")
    public SolicitudAnalisis informe(@PathVariable String id, @Valid @RequestBody DictamenRequest request) {
        return servicio.dictaminar(id, TipoAnalisis.DIGITAL, request);
    }
}
