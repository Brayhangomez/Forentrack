package com.forentrack.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Respuesta del microservicio (las fechas llegan como texto ISO). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ValidacionDto(String id, String evidenciaId, String evidenciaCodigo,
                            String resultado, String observacion, String revisor, String fecha) {
}
