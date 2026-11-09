package com.forentrack.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Respuesta del microservicio (las fechas llegan como texto ISO). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoginResponseDto(String token, String tipo, Long expiraEnMinutos, String nombreCompleto, String rol, String correo) {
}
