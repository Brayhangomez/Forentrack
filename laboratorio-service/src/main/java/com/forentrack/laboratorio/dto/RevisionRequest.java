package com.forentrack.laboratorio.dto;

import jakarta.validation.constraints.Size;

/** Observación del jefe de laboratorio (obligatoria al rechazar; se valida en el servicio). */
public record RevisionRequest(
        @Size(max = 1000, message = "La observación no puede superar 1000 caracteres")
        String observacion) {
}
