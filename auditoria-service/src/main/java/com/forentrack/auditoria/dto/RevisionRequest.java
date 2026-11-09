package com.forentrack.auditoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevisionRequest(
        @NotBlank(message = "La observación es obligatoria")
        @Size(max = 1000, message = "La observación no puede superar 1000 caracteres")
        String observacion) {
}
