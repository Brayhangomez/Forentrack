package com.forentrack.web.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Respuesta del microservicio (las fechas llegan como texto ISO). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SolicitudDto(String id, String codigo, String evidenciaId,
                           String evidenciaCodigo, String casoCodigo, String tipoEvidencia, String tipoAnalisis,
                           String objetivo, String prioridad, String estado, String solicitadaPor,
                           String fechaSolicitud, String asignadoA, String asignadoNombre, String asignadoPor,
                           String fechaAsignacion, String fechaInicio, String hallazgos, String conclusiones,
                           String hashSha256, String herramienta, String fechaDictamen, String observacionRevision,
                           String revisadoPor, String fechaRevision, List<CambioEstadoDto> historial) {

    public SolicitudDto {
        historial = historial == null ? List.of() : historial;
    }
}
