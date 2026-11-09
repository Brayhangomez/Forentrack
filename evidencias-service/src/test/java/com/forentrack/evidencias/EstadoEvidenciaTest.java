package com.forentrack.evidencias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.forentrack.evidencias.model.EstadoEvidencia;
import com.forentrack.evidencias.model.Rol;

/** La máquina de estados asigna cada transición a un único tipo de rol. */
class EstadoEvidenciaTest {

    @Test
    void transicionesPorRol() {
        assertEquals(Set.of(Rol.CUSTODIO), EstadoEvidencia.REGISTRADA.rolesPara(EstadoEvidencia.EN_CUSTODIA));
        assertEquals(Set.of(Rol.PERITO, Rol.ANALISTA_DIGITAL),
                EstadoEvidencia.EN_CUSTODIA.rolesPara(EstadoEvidencia.EN_ANALISIS));
        assertEquals(Set.of(Rol.JEFE_LABORATORIO), EstadoEvidencia.EN_ANALISIS.rolesPara(EstadoEvidencia.ANALIZADA));
        assertEquals(Set.of(Rol.CUSTODIO), EstadoEvidencia.ANALIZADA.rolesPara(EstadoEvidencia.DEVUELTA));
        assertEquals(Set.of(Rol.REVISOR_LEGAL), EstadoEvidencia.EN_CUSTODIA.rolesPara(EstadoEvidencia.DESCARTADA));
    }

    @Test
    void estadosFinalesNoTienenSalida() {
        for (EstadoEvidencia destino : EstadoEvidencia.values()) {
            assertTrue(EstadoEvidencia.DEVUELTA.rolesPara(destino).isEmpty());
            assertTrue(EstadoEvidencia.DESCARTADA.rolesPara(destino).isEmpty());
        }
    }

    @Test
    void noSePuedeSaltarLaCustodia() {
        assertTrue(EstadoEvidencia.REGISTRADA.rolesPara(EstadoEvidencia.EN_ANALISIS).isEmpty());
        assertTrue(EstadoEvidencia.EN_ANALISIS.rolesPara(EstadoEvidencia.DESCARTADA).isEmpty());
        assertEquals(EnumSet.noneOf(Rol.class), EstadoEvidencia.REGISTRADA.rolesPara(EstadoEvidencia.DEVUELTA));
    }
}
