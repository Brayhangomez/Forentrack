package com.forentrack.evidencias.dto;

import com.forentrack.evidencias.model.EstadoEvidencia;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cambio de estado solicitado por cadena-custodia-service o laboratorio-service. */
public record CambioEstadoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoEvidencia estado,

        @Size(max = 500, message = "La observación no puede superar 500 caracteres")
        String observacion) {
}
