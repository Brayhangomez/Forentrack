package com.forentrack.casos;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.forentrack.casos.model.EstadoCaso;

class EstadoCasoTest {

    @Test
    void soloLosEstadosActivosAdmitenEvidencias() {
        assertTrue(EstadoCaso.ABIERTO.admiteEvidencias());
        assertTrue(EstadoCaso.ASIGNADO.admiteEvidencias());
        assertTrue(EstadoCaso.EN_INVESTIGACION.admiteEvidencias());
        assertFalse(EstadoCaso.SUSPENDIDO.admiteEvidencias());
        assertFalse(EstadoCaso.CERRADO.admiteEvidencias());
    }
}
