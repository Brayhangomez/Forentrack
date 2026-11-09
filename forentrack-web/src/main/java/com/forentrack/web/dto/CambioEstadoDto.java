package com.forentrack.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Respuesta del microservicio (las fechas llegan como texto ISO). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CambioEstadoDto(String desde, String hacia, String usuario, String rol, String observacion, String fecha) {
}
