package com.forentrack.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Respuesta del microservicio (las fechas llegan como texto ISO). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MovimientoDto(String id, String evidenciaId, String evidenciaCodigo,
                            String casoCodigo, String tipo, String responsable, String rolResponsable,
                            String origen, String destino, String observacion, String fecha) {
}
