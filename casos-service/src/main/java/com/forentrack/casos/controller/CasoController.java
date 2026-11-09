package com.forentrack.casos.controller;

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

import com.forentrack.casos.dto.AsignacionRequest;
import com.forentrack.casos.dto.CasoRequest;
import com.forentrack.casos.dto.CasoResponse;
import com.forentrack.casos.dto.ConceptoRequest;
import com.forentrack.casos.dto.ObservacionRequest;
import com.forentrack.casos.dto.TextoRequest;
import com.forentrack.casos.exception.ErrorResponse;
import com.forentrack.casos.model.EstadoCaso;
import com.forentrack.casos.service.CasoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/casos")
@Tag(name = "Casos", description = "Registro, asignación, investigación, concepto legal y cierre de casos")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado o token expirado"),
    @ApiResponse(responseCode = "403", description = "El rol no tiene permiso"),
    @ApiResponse(responseCode = "404", description = "Caso no encontrado"),
    @ApiResponse(responseCode = "409", description = "El estado actual no permite la acción")
})
public class CasoController {

    private final CasoService casoService;

    public CasoController(CasoService casoService) {
        this.casoService = casoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INVESTIGADOR')")
    @Operation(summary = "Registrar un caso", description = "Rol exclusivo: INVESTIGADOR")
    public CasoResponse crear(@Valid @RequestBody CasoRequest request) {
        return casoService.crear(request);
    }

    @GetMapping
    @Operation(summary = "Buscar casos por código, estado y/o investigador asignado",
               description = "Todos los roles. Sin filtros devuelve todos, del más reciente al más antiguo")
    public List<CasoResponse> buscar(@RequestParam(required = false) String codigo,
                                     @RequestParam(required = false) EstadoCaso estado,
                                     @RequestParam(required = false) String investigador) {
        return casoService.buscar(codigo, estado, investigador);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un caso por id", description = "Todos los roles")
    public CasoResponse obtener(@PathVariable String id) {
        return casoService.obtener(id);
    }

    @PostMapping("/{id}/asignacion")
    @PreAuthorize("hasRole('FISCAL')")
    @Operation(summary = "Asignar investigador (ABIERTO/ASIGNADO → ASIGNADO)", description = "Rol exclusivo: FISCAL")
    public CasoResponse asignar(@PathVariable String id, @Valid @RequestBody AsignacionRequest request) {
        return casoService.asignar(id, request.investigadorCorreo());
    }

    @PostMapping("/{id}/inicio-investigacion")
    @PreAuthorize("hasRole('INVESTIGADOR')")
    @Operation(summary = "Iniciar investigación (ASIGNADO → EN_INVESTIGACION)",
               description = "Rol exclusivo: INVESTIGADOR (solo el asignado)")
    public CasoResponse iniciar(@PathVariable String id) {
        return casoService.iniciarInvestigacion(id);
    }

    @PostMapping("/{id}/notas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INVESTIGADOR')")
    @Operation(summary = "Agregar nota de investigación", description = "Rol exclusivo: INVESTIGADOR (solo el asignado)")
    public CasoResponse nota(@PathVariable String id, @Valid @RequestBody TextoRequest request) {
        return casoService.agregarNota(id, request.texto());
    }

    @PostMapping("/{id}/suspension")
    @PreAuthorize("hasRole('FISCAL')")
    @Operation(summary = "Suspender caso (→ SUSPENDIDO)", description = "Rol exclusivo: FISCAL")
    public CasoResponse suspender(@PathVariable String id, @Valid @RequestBody TextoRequest request) {
        return casoService.suspender(id, request.texto());
    }

    @PostMapping("/{id}/reactivacion")
    @PreAuthorize("hasRole('FISCAL')")
    @Operation(summary = "Reactivar caso suspendido", description = "Rol exclusivo: FISCAL")
    public CasoResponse reactivar(@PathVariable String id,
                                  @Valid @RequestBody(required = false) ObservacionRequest request) {
        return casoService.reactivar(id, request != null ? request.observacion() : null);
    }

    @PostMapping("/{id}/concepto-legal")
    @PreAuthorize("hasRole('REVISOR_LEGAL')")
    @Operation(summary = "Emitir concepto legal (FAVORABLE/DESFAVORABLE)", description = "Rol exclusivo: REVISOR_LEGAL")
    public CasoResponse concepto(@PathVariable String id, @Valid @RequestBody ConceptoRequest request) {
        return casoService.emitirConcepto(id, request.concepto(), request.fundamento());
    }

    @PostMapping("/{id}/cierre")
    @PreAuthorize("hasRole('FISCAL')")
    @Operation(summary = "Cerrar caso (EN_INVESTIGACION → CERRADO, requiere concepto FAVORABLE)",
               description = "Rol exclusivo: FISCAL")
    public CasoResponse cerrar(@PathVariable String id,
                               @Valid @RequestBody(required = false) ObservacionRequest request) {
        return casoService.cerrar(id, request != null ? request.observacion() : null);
    }
}
