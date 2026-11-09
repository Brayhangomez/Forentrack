package com.forentrack.laboratorio.dto;

import com.forentrack.laboratorio.model.Prioridad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudRequest(
        @NotBlank(message = "La evidencia es obligatoria")
        String evidenciaId,

        @NotBlank(message = "El objetivo del análisis es obligatorio")
        @Size(max = 1000, message = "El objetivo no puede superar 1000 caracteres")
        String objetivo,

        @NotNull(message = "La prioridad es obligatoria")
        Prioridad prioridad) {
}
