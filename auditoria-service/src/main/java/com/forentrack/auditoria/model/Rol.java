package com.forentrack.auditoria.model;

/**
 * Roles de ForenTrack. Este enum es idéntico en todos los servicios (mismos nombres y orden)
 * porque cada microservicio se despliega solo y no comparte librerías con los demás.
 */
public enum Rol {
    INVESTIGADOR("Investigador"),
    PERITO("Perito"),
    CUSTODIO("Custodio de evidencias"),
    ADMINISTRADOR("Administrador"),
    FISCAL("Fiscal"),
    AUDITOR("Auditor"),
    TECNICO_CAMPO("Técnico de campo"),
    ANALISTA_DIGITAL("Analista digital"),
    JEFE_LABORATORIO("Jefe de laboratorio"),
    REVISOR_LEGAL("Revisor legal");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Convierte un texto en rol sin lanzar excepción; devuelve null si no corresponde a ningún rol. */
    public static Rol desde(String valor) {
        if (valor == null) {
            return null;
        }
        for (Rol r : values()) {
            if (r.name().equals(valor)) {
                return r;
            }
        }
        return null;
    }
}
