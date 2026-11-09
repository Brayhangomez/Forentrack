package com.forentrack.auditoria.dto;

import java.util.Map;

/** Indicadores para el tablero del AUDITOR. */
public record ResumenResponse(long total, long exitosos, long fallidos, long denegados,
                              long pendientesRevision, Map<String, Long> porServicio,
                              Map<String, Long> accionesFrecuentes) {
}
