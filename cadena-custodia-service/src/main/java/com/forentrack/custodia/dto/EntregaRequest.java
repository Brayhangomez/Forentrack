package com.forentrack.custodia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EntregaRequest(
        @NotBlank(message = "La evidencia es obligatoria")
        String evidenciaId,

        @Size(max = 200, message = "El destino no puede superar 200 caracteres")
        String destino,

        @Size(max = 1000, message = "La observación no puede superar 1000 caracteres")
        String observacion) {
}
