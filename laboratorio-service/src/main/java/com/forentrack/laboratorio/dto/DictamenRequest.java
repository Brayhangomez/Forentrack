package com.forentrack.laboratorio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dictamen pericial o informe técnico digital. */
public record DictamenRequest(
        @NotBlank(message = "Los hallazgos son obligatorios")
        @Size(max = 4000, message = "Los hallazgos no pueden superar 4000 caracteres")
        String hallazgos,

        @NotBlank(message = "Las conclusiones son obligatorias")
        @Size(max = 4000, message = "Las conclusiones no pueden superar 4000 caracteres")
        String conclusiones) {
}
