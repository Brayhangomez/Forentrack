package com.forentrack.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Respuesta del microservicio (las fechas llegan como texto ISO). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoDto(String id, String fecha, String servicio, String accion,
                        String resultado, String usuario, String rol, String recursoTipo, String recursoId,
                        String detalle, Boolean revisado, String revisadoPor, String observacionRevision,
                        String fechaRevision) {
}
