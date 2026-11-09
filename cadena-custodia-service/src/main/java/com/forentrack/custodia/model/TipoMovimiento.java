package com.forentrack.custodia.model;

/** Tipos de eslabón de la cadena de custodia. */
public enum TipoMovimiento {
    /** El técnico de campo entrega la evidencia embalada a custodia. */
    ENTREGA,
    /** El custodio la recibe e ingresa a bodega (evidencia → EN_CUSTODIA). */
    RECEPCION,
    /** El custodio la reubica o la traslada (por ejemplo, al laboratorio). */
    TRASLADO,
    /** El custodio la devuelve a su destinatario final (evidencia → DEVUELTA). */
    DEVOLUCION
}
