package com.forentrack.casos.model;

import java.time.LocalDateTime;

/** Entrada del historial de estados del caso (quién, cuándo y por qué). */
public record CambioEstado(String desde, String hacia, String usuario, String rol, String observacion,
                           LocalDateTime fecha) {
}
