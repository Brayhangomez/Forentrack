package com.forentrack.usuarios.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoRequest(
        @NotNull(message = "El campo activo es obligatorio")
        Boolean activo) {
}