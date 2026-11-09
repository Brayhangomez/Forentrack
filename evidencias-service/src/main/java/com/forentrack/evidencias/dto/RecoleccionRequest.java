package com.forentrack.evidencias.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/** Acta de recolección que completa el TECNICO_CAMPO. */
public record RecoleccionRequest(
        @NotBlank(message = "El lugar del hallazgo es obligatorio")
        @Size(max = 200, message = "El lugar no puede superar 200 caracteres")
        String lugarHallazgo,

        @NotNull(message = "La fecha del hallazgo es obligatoria")
        @PastOrPresent(message = "La fecha del hallazgo no puede ser futura")
        LocalDateTime fechaHallazgo,

        @NotBlank(message = "El tipo de embalaje es obligatorio")
        @Size(max = 200, message = "El embalaje no puede superar 200 caracteres")
        String embalaje,

        @Size(max = 1000, message = "Las observaciones no pueden superar 1000 caracteres")
        String observaciones) {
}
