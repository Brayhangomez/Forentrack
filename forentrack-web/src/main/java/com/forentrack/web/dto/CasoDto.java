package com.forentrack.web.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Respuesta del microservicio (las fechas llegan como texto ISO). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CasoDto(String id, String codigo, String titulo, String descripcion,
                      String estado, String creadoPor, String fechaCreacion, String investigadorAsignado,
                      String investigadorNombre, String asignadoPor, String motivoSuspension, String conceptoLegal,
                      String fundamentoLegal, String revisorLegal, String fechaConcepto, List<NotaDto> notas,
                      List<CambioEstadoDto> historial, String fechaActualizacion) {

    public CasoDto {
        notas = notas == null ? List.of() : notas;
        historial = historial == null ? List.of() : historial;
    }
}
