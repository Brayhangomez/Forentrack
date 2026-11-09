package com.forentrack.laboratorio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AsignacionRequest(
        @NotBlank(message = "El responsable es obligatorio")
        @Email(message = "El correo del responsable no es válido")
        String responsableCorreo) {
}
