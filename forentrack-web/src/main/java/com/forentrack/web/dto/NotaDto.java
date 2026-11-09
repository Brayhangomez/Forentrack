package com.forentrack.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Respuesta del microservicio (las fechas llegan como texto ISO). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotaDto(String texto, String autor, String fecha) {
}
