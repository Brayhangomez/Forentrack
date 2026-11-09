package com.forentrack.custodia.dto;

import com.forentrack.custodia.model.ResultadoValidacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ValidacionRequest(
        @NotNull(message = "El resultado es obligatorio")
        ResultadoValidacion resultado,

        @NotBlank(message = "La observación es obligatoria")
        @Size(max = 1000, message = "La observación no puede superar 1000 caracteres")
        String observacion) {
}
