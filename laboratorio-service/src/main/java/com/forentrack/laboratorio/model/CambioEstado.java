package com.forentrack.laboratorio.model;

import java.time.LocalDateTime;

/** Entrada del historial de la solicitud. */
public record CambioEstado(String desde, String hacia, String usuario, String rol, String observacion,
                           LocalDateTime fecha) {
}
