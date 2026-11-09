package com.forentrack.evidencias.model;

import java.time.LocalDateTime;

/** Entrada del historial de estados de la evidencia. */
public record CambioEstado(String desde, String hacia, String usuario, String rol, String observacion,
                           LocalDateTime fecha) {
}
