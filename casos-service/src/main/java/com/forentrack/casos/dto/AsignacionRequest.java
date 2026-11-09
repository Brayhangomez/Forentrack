package com.forentrack.casos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AsignacionRequest(
        @NotBlank(message = "El investigador es obligatorio")
        @Email(message = "El correo del investigador no es válido")
        String investigadorCorreo) {
}
