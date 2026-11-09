package com.forentrack.auditoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Evento enviado por los demás servicios (con cabecera X-Internal-Key). */
public record EventoRequest(
        @NotBlank(message = "El servicio es obligatorio") @Size(max = 80) String servicio,
        @NotBlank(message = "La acción es obligatoria") @Size(max = 80) String accion,
        @NotBlank(message = "El resultado es obligatorio")
        @Pattern(regexp = "EXITO|FALLIDO|DENEGADO", message = "Resultado no válido") String resultado,
        @Size(max = 160) String usuario,
        @Size(max = 40) String rol,
        @Size(max = 60) String recursoTipo,
        @Size(max = 200) String recursoId,
        @Size(max = 1000) String detalle) {
}
