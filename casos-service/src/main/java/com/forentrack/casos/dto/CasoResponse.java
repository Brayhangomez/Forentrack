package com.forentrack.casos.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.forentrack.casos.model.CambioEstado;
import com.forentrack.casos.model.ConceptoLegal;
import com.forentrack.casos.model.EstadoCaso;
import com.forentrack.casos.model.Nota;

/** Los 7 primeros campos son los de la versión 1 (compatibilidad); el resto es nuevo. */
public record CasoResponse(
        String id,
        String codigo,
        String titulo,
        String descripcion,
        EstadoCaso estado,
        String creadoPor,
        LocalDateTime fechaCreacion,
        String investigadorAsignado,
        String investigadorNombre,
        String asignadoPor,
        String motivoSuspension,
        ConceptoLegal conceptoLegal,
        String fundamentoLegal,
        String revisorLegal,
        LocalDateTime fechaConcepto,
        List<Nota> notas,
        List<CambioEstado> historial,
        LocalDateTime fechaActualizacion) {
}
