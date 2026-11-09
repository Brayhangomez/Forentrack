package com.forentrack.evidencias.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.forentrack.evidencias.model.CambioEstado;
import com.forentrack.evidencias.model.EstadoEvidencia;
import com.forentrack.evidencias.model.TipoEvidencia;

/** Los 9 primeros campos son los de la versión 1 (compatibilidad); el resto es nuevo. */
public record EvidenciaResponse(
        String id,
        String codigo,
        String casoId,
        String casoCodigo,
        TipoEvidencia tipo,
        String descripcion,
        EstadoEvidencia estadoActual,
        String registradoPor,
        LocalDateTime fechaRegistro,
        String lugarHallazgo,
        LocalDateTime fechaHallazgo,
        String embalaje,
        String observacionesRecoleccion,
        boolean actaCompleta,
        String motivoDescarte,
        List<CambioEstado> historial,
        LocalDateTime fechaActualizacion) {
}
