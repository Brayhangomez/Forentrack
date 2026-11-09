package com.forentrack.evidencias.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Datos mínimos del caso que devuelve casos-service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CasoRef(String id, String codigo, String estado) {
}
