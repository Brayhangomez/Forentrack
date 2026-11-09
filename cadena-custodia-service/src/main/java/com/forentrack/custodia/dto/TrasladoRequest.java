package com.forentrack.custodia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrasladoRequest(
        @NotBlank(message = "La evidencia es obligatoria")
        String evidenciaId,

        @NotBlank(message = "El destino es obligatorio")
        @Size(max = 200, message = "El destino no puede superar 200 caracteres")
        String destino,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 1000, message = "El motivo no puede superar 1000 caracteres")
        String motivo) {
}
