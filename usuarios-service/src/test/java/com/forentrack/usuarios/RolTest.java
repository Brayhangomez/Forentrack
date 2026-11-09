package com.forentrack.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.forentrack.usuarios.model.Rol;

/** Prueba unitaria sin MongoDB: el enum de roles debe ser el mismo en todos los servicios. */
class RolTest {

    @Test
    void existenExactamenteDiezRoles() {
        assertEquals(10, Rol.values().length);
        assertEquals("INVESTIGADOR,PERITO,CUSTODIO,ADMINISTRADOR,FISCAL,AUDITOR,TECNICO_CAMPO,"
                + "ANALISTA_DIGITAL,JEFE_LABORATORIO,REVISOR_LEGAL",
                String.join(",", java.util.Arrays.stream(Rol.values()).map(Enum::name).toList()));
    }

    @Test
    void desdeNoLanzaExcepcion() {
        assertEquals(Rol.FISCAL, Rol.desde("FISCAL"));
        assertNull(Rol.desde("SUPERUSUARIO"));
        assertNull(Rol.desde(null));
    }
}
