package com.forentrack.web.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Respuesta del microservicio (las fechas llegan como texto ISO). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ErrorDto(Integer estado, String mensaje, Map<String, String> errores, String fecha) {
}
