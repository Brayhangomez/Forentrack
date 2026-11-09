package com.forentrack.web.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Respuesta del microservicio (las fechas llegan como texto ISO). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResumenDto(Long total, Long exitosos, Long fallidos, Long denegados,
                         Long pendientesRevision, Map<String, Long> porServicio,
                         Map<String, Long> accionesFrecuentes) {

    public ResumenDto {
        porServicio = porServicio == null ? Map.of() : porServicio;
        accionesFrecuentes = accionesFrecuentes == null ? Map.of() : accionesFrecuentes;
    }
}
