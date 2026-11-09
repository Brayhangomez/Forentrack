package com.forentrack.casos.dto;

import jakarta.validation.constraints.Size;

/** Observación opcional (reactivación o cierre). */
public record ObservacionRequest(
        @Size(max = 1000, message = "La observación no puede superar 1000 caracteres")
        String observacion) {
}
