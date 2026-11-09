package com.forentrack.web.dto;

/** Elemento de la bandeja de trabajo del panel (lo que el rol tiene pendiente). */
public record ItemBandeja(String titulo, String detalle, String estado, String enlace) {
}
