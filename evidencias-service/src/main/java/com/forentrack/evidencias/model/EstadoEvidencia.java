package com.forentrack.evidencias.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de la evidencia y qué rol puede hacer cada transición (máquina de estados única del sistema).
 * REGISTRADA -> EN_CUSTODIA (CUSTODIO) -> EN_ANALISIS (PERITO o ANALISTA_DIGITAL según el tipo)
 * -> ANALIZADA (JEFE_LABORATORIO) -> DEVUELTA (CUSTODIO). DESCARTADA (REVISOR_LEGAL) desde
 * REGISTRADA, EN_CUSTODIA o ANALIZADA. DEVUELTA y DESCARTADA son finales.
 */
public enum EstadoEvidencia {
    REGISTRADA,
    EN_CUSTODIA,
    EN_ANALISIS,
    ANALIZADA,
    DEVUELTA,
    DESCARTADA;

    /** Roles que pueden llevar la evidencia desde este estado al destino (vacío si la transición no existe). */
    public Set<Rol> rolesPara(EstadoEvidencia destino) {
        if (destino == DESCARTADA && (this == REGISTRADA || this == EN_CUSTODIA || this == ANALIZADA)) {
            return EnumSet.of(Rol.REVISOR_LEGAL);
        }
        return switch (this) {
            case REGISTRADA -> destino == EN_CUSTODIA ? EnumSet.of(Rol.CUSTODIO) : EnumSet.noneOf(Rol.class);
            case EN_CUSTODIA -> destino == EN_ANALISIS
                    ? EnumSet.of(Rol.PERITO, Rol.ANALISTA_DIGITAL) : EnumSet.noneOf(Rol.class);
            case EN_ANALISIS -> destino == ANALIZADA ? EnumSet.of(Rol.JEFE_LABORATORIO) : EnumSet.noneOf(Rol.class);
            case ANALIZADA -> destino == DEVUELTA ? EnumSet.of(Rol.CUSTODIO) : EnumSet.noneOf(Rol.class);
            case DEVUELTA, DESCARTADA -> EnumSet.noneOf(Rol.class);
        };
    }

    public boolean esFinal() {
        return this == DEVUELTA || this == DESCARTADA;
    }
}
