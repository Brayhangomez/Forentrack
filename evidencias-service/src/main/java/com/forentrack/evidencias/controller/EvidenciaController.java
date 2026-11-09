package com.forentrack.evidencias.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.forentrack.evidencias.dto.CambioEstadoRequest;
import com.forentrack.evidencias.dto.EvidenciaRequest;
import com.forentrack.evidencias.dto.EvidenciaResponse;
import com.forentrack.evidencias.dto.MotivoRequest;
import com.forentrack.evidencias.dto.RecoleccionRequest;
import com.forentrack.evidencias.exception.ErrorResponse;
import com.forentrack.evidencias.model.EstadoEvidencia;
import com.forentrack.evidencias.model.TipoEvidencia;
import com.forentrack.evidencias.service.EvidenciaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/evidencias")
@Tag(name = "Evidencias", description = "Registro, acta de recolección, estados y descarte de evidencias")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado o token expirado"),
    @ApiResponse(responseCode = "403", description = "El rol no tiene permiso"),
    @ApiResponse(responseCode = "404", description = "Evidencia o caso no encontrado"),
    @ApiResponse(responseCode = "409", description = "El estado actual no permite la acción"),
    @ApiResponse(responseCode = "503", description = "casos-service no disponible")
})
public class EvidenciaController {

    private final EvidenciaService evidenciaService;

    public EvidenciaController(EvidenciaService evidenciaService) {
        this.evidenciaService = evidenciaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TECNICO_CAMPO')")
    @Operation(summary = "Registrar una evidencia en un caso activo",
               description = "Rol exclusivo: TECNICO_CAMPO. Genera el código EV-AAAA-NNNNN y valida el caso en casos-service.")
    public EvidenciaResponse registrar(@Valid @RequestBody EvidenciaRequest request) {
        return evidenciaService.registrar(request);
    }

    @GetMapping
    @Operation(summary = "Listar evidencias filtrando por caso, estado y/o tipo", description = "Todos los roles")
    public List<EvidenciaResponse> listar(@RequestParam(required = false) String casoId,
                                          @RequestParam(required = false) EstadoEvidencia estado,
                                          @RequestParam(required = false) TipoEvidencia tipo) {
        return evidenciaService.listar(casoId, estado, tipo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una evidencia por id", description = "Todos los roles")
    public EvidenciaResponse obtener(@PathVariable String id) {
        return evidenciaService.obtener(id);
    }

    @PutMapping("/{id}/recoleccion")
    @PreAuthorize("hasRole('TECNICO_CAMPO')")
    @Operation(summary = "Completar el acta de recolección (solo REGISTRADA)", description = "Rol exclusivo: TECNICO_CAMPO")
    public EvidenciaResponse recoleccion(@PathVariable String id, @Valid @RequestBody RecoleccionRequest request) {
        return evidenciaService.completarRecoleccion(id, request);
    }

    @PostMapping("/{id}/descarte")
    @PreAuthorize("hasRole('REVISOR_LEGAL')")
    @Operation(summary = "Autorizar descarte (→ DESCARTADA)", description = "Rol exclusivo: REVISOR_LEGAL")
    public EvidenciaResponse descartar(@PathVariable String id, @Valid @RequestBody MotivoRequest request) {
        return evidenciaService.descartar(id, request.motivo());
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('CUSTODIO','PERITO','ANALISTA_DIGITAL','JEFE_LABORATORIO')")
    @Operation(summary = "Cambio de estado interno (solo servicios)",
               description = "Lo invocan cadena-custodia-service y laboratorio-service con la cabecera X-Internal-Key "
                       + "y el JWT del usuario. La transición se valida contra el rol (ver EstadoEvidencia).")
    public EvidenciaResponse cambiarEstado(@PathVariable String id,
                                           @Valid @RequestBody CambioEstadoRequest request,
                                           @Parameter(hidden = true)
                                           @RequestHeader(value = "X-Internal-Key", required = false) String clave) {
        return evidenciaService.cambiarEstadoInterno(id, request.estado(), request.observacion(), clave);
    }
}
