package com.forentrack.casos.model;

/**
 * Ciclo de vida del caso. Los tres estados originales se conservan para no romper datos existentes.
 * ABIERTO -> ASIGNADO (FISCAL) -> EN_INVESTIGACION (INVESTIGADOR) <-> SUSPENDIDO (FISCAL) -> CERRADO (FISCAL)
 */
public enum EstadoCaso {
    ABIERTO,
    ASIGNADO,
    EN_INVESTIGACION,
    SUSPENDIDO,
    CERRADO;

    /** Un caso admite nuevas evidencias mientras no esté suspendido ni cerrado. */
    public boolean admiteEvidencias() {
        return this == ABIERTO || this == ASIGNADO || this == EN_INVESTIGACION;
    }
}
