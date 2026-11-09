package com.forentrack.auditoria.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** Formato único de error que devuelven todos los servicios de ForenTrack. */
public record ErrorResponse(
        int estado,
        String mensaje,
        Map<String, String> errores,
        LocalDateTime fecha) {
}
