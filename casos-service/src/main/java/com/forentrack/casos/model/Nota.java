package com.forentrack.casos.model;

import java.time.LocalDateTime;

/** Nota de investigación embebida en el caso. */
public record Nota(String texto, String autor, LocalDateTime fecha) {
}
