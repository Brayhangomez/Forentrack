package com.forentrack.custodia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DevolucionRequest(
        @NotBlank(message = "La evidencia es obligatoria")
        String evidenciaId,

        @NotBlank(message = "El destinatario es obligatorio")
        @Size(max = 200, message = "El destinatario no puede superar 200 caracteres")
        String destinatario,

        @Size(max = 1000, message = "La observación no puede superar 1000 caracteres")
        String observacion) {
}
