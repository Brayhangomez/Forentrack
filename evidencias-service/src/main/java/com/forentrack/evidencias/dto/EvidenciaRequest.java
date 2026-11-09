package com.forentrack.evidencias.dto;

import com.forentrack.evidencias.model.TipoEvidencia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EvidenciaRequest(
        @NotBlank(message = "El caso es obligatorio")
        String casoId,

        @NotNull(message = "El tipo de evidencia es obligatorio")
        TipoEvidencia tipo,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
        String descripcion) {
}