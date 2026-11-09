package com.forentrack.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Respuesta del microservicio (las fechas llegan como texto ISO). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsuarioDto(String id, String nombreCompleto, String correo, String rol, Boolean activo, String fechaCreacion) {
}
