package com.forentrack.custodia.controller;

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

import com.forentrack.custodia.dto.DevolucionRequest;
import com.forentrack.custodia.dto.EntregaRequest;
import com.forentrack.custodia.dto.RecepcionRequest;
import com.forentrack.custodia.dto.TrasladoRequest;
import com.forentrack.custodia.dto.ValidacionRequest;
import com.forentrack.custodia.exception.ErrorResponse;
import com.forentrack.custodia.model.Movimiento;
import com.forentrack.custodia.model.TipoMovimiento;
import com.forentrack.custodia.model.ValidacionCadena;
import com.forentrack.custodia.service.CustodiaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/custodia")
@Tag(name = "Cadena de custodia", description = "Entregas, recepciones, traslados, devoluciones y validación legal")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado o token expirado"),
    @ApiResponse(responseCode = "403", description = "El rol no tiene permiso"),
    @ApiResponse(responseCode = "404", description = "Evidencia no encontrada"),
    @ApiResponse(responseCode = "409", description = "El estado de la evidencia no permite la acción"),
    @ApiResponse(responseCode = "503", description = "evidencias-service no disponible")
})
public class CustodiaController {

    private final CustodiaService custodiaService;

    public CustodiaController(CustodiaService custodiaService) {
        this.custodiaService = custodiaService;
    }

    @PostMapping("/entregas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TECNICO_CAMPO')")
    @Operation(summary = "Entregar evidencia a custodia (requiere acta completa)", description = "Rol exclusivo: TECNICO_CAMPO")
    public Movimiento entregar(@Valid @RequestBody EntregaRequest request) {
        return custodiaService.entregar(request);
    }

    @PostMapping("/recepciones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTODIO')")
    @Operation(summary = "Recibir evidencia en bodega (REGISTRADA → EN_CUSTODIA)", description = "Rol exclusivo: CUSTODIO")
    public Movimiento recibir(@Valid @RequestBody RecepcionRequest request) {
        return custodiaService.recibir(request);
    }

    @PostMapping("/traslados")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTODIO')")
    @Operation(summary = "Trasladar o reubicar evidencia", description = "Rol exclusivo: CUSTODIO")
    public Movimiento trasladar(@Valid @RequestBody TrasladoRequest request) {
        return custodiaService.trasladar(request);
    }

    @PostMapping("/devoluciones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTODIO')")
    @Operation(summary = "Devolver evidencia analizada (ANALIZADA → DEVUELTA, requiere cadena VALIDA)",
               description = "Rol exclusivo: CUSTODIO")
    public Movimiento devolver(@Valid @RequestBody DevolucionRequest request) {
        return custodiaService.devolver(request);
    }

    @PostMapping("/evidencias/{evidenciaId}/validaciones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('REVISOR_LEGAL')")
    @Operation(summary = "Validar u observar la cadena de custodia", description = "Rol exclusivo: REVISOR_LEGAL")
    public ValidacionCadena validar(@PathVariable String evidenciaId, @Valid @RequestBody ValidacionRequest request) {
        return custodiaService.validar(evidenciaId, request);
    }

    @GetMapping("/evidencias/{evidenciaId}/movimientos")
    @Operation(summary = "Cadena de custodia de una evidencia (orden cronológico)")
    public List<Movimiento> historial(@PathVariable String evidenciaId) {
        return custodiaService.historial(evidenciaId);
    }

    @GetMapping("/evidencias/{evidenciaId}/validaciones")
    @Operation(summary = "Validaciones legales de una evidencia (la más reciente primero)")
    public List<ValidacionCadena> validaciones(@PathVariable String evidenciaId) {
        return custodiaService.validaciones(evidenciaId);
    }

    @GetMapping("/movimientos")
    @Operation(summary = "Últimos 200 movimientos, opcionalmente por tipo")
    public List<Movimiento> recientes(@RequestParam(required = false) TipoMovimiento tipo) {
        return custodiaService.recientes(tipo);
    }

    @GetMapping("/pendientes")
    @Operation(summary = "Entregas pendientes de recepción en bodega")
    public List<Movimiento> pendientes() {
        return custodiaService.pendientesDeRecepcion();
    }
}
