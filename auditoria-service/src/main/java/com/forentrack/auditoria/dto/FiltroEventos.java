package com.forentrack.auditoria.dto;

import java.time.LocalDate;

/** Filtros opcionales de la bitácora. */
public record FiltroEventos(String usuario, String accion, String servicio, String resultado,
                            LocalDate desde, LocalDate hasta, Boolean revisado) {
}
