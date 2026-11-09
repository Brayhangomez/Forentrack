package com.forentrack.laboratorio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** PERICIAL lo realiza un PERITO (evidencia física o documental); DIGITAL, un ANALISTA_DIGITAL. */
public enum TipoAnalisis {
    PERICIAL(Rol.PERITO),
    DIGITAL(Rol.ANALISTA_DIGITAL);

    private final Rol responsable;

    TipoAnalisis(Rol responsable) {
        this.responsable = responsable;
    }

    @JsonIgnore
    public Rol getResponsable() {
        return responsable;
    }

    public static TipoAnalisis segunTipoEvidencia(String tipoEvidencia) {
        return "DIGITAL".equals(tipoEvidencia) ? DIGITAL : PERICIAL;
    }
}
