package com.forentrack.casos.dto;

import com.forentrack.casos.model.ConceptoLegal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConceptoRequest(
        @NotNull(message = "El concepto es obligatorio")
        ConceptoLegal concepto,

        @NotBlank(message = "El fundamento es obligatorio")
        @Size(max = 2000, message = "El fundamento no puede superar 2000 caracteres")
        String fundamento) {
}
