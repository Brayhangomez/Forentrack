package com.forentrack.evidencias.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MotivoRequest(
        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 1000, message = "El motivo no puede superar 1000 caracteres")
        String motivo) {
}
