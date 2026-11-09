package com.forentrack.web.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Respuesta del microservicio (las fechas llegan como texto ISO). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EvidenciaDto(String id, String codigo, String casoId, String casoCodigo,
                           String tipo, String descripcion, String estadoActual, String registradoPor,
                           String fechaRegistro, String lugarHallazgo, String fechaHallazgo, String embalaje,
                           String observacionesRecoleccion, Boolean actaCompleta, String motivoDescarte,
                           List<CambioEstadoDto> historial, String fechaActualizacion) {

    public EvidenciaDto {
        historial = historial == null ? List.of() : historial;
    }
}
