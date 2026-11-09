package com.forentrack.casos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Texto obligatorio (nota de investigación o motivo de suspensión). */
public record TextoRequest(
        @NotBlank(message = "El texto es obligatorio")
        @Size(max = 2000, message = "El texto no puede superar 2000 caracteres")
        String texto) {
}
