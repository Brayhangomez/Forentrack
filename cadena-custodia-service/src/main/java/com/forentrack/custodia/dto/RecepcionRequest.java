package com.forentrack.custodia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecepcionRequest(
        @NotBlank(message = "La evidencia es obligatoria")
        String evidenciaId,

        @NotBlank(message = "La ubicación en bodega es obligatoria")
        @Size(max = 200, message = "La ubicación no puede superar 200 caracteres")
        String ubicacion,

        @Size(max = 1000, message = "La observación no puede superar 1000 caracteres")
        String observacion) {
}
