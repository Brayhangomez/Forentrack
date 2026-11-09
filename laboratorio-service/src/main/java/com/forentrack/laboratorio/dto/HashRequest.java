package com.forentrack.laboratorio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Huella SHA-256 de la imagen forense adquirida (garantiza integridad). */
public record HashRequest(
        @NotBlank(message = "El hash es obligatorio")
        @Pattern(regexp = "^[a-fA-F0-9]{64}$", message = "El hash SHA-256 debe tener 64 caracteres hexadecimales")
        String hashSha256,

        @NotBlank(message = "La herramienta de adquisición es obligatoria")
        @Size(max = 120, message = "La herramienta no puede superar 120 caracteres")
        String herramienta) {
}
