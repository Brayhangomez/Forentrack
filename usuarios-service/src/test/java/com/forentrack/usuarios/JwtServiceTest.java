package com.forentrack.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.forentrack.usuarios.model.Rol;
import com.forentrack.usuarios.model.Usuario;
import com.forentrack.usuarios.security.JwtService;

/** Prueba unitaria sin MongoDB ni contexto de Spring. */
class JwtServiceTest {

    private static final String SECRETO = "clave-de-prueba-solo-para-tests-0123456789";

    @Test
    void generaYLeeElToken() {
        JwtService jwt = new JwtService(SECRETO, 5);
        Usuario u = new Usuario();
        u.setId("u1");
        u.setCorreo("fiscal@forentrack.test");
        u.setRol(Rol.FISCAL);
        var claims = jwt.leer(jwt.generar(u));
        assertEquals("u1", claims.getSubject());
        assertEquals("FISCAL", claims.get("rol", String.class));
        assertEquals("fiscal@forentrack.test", claims.get("correo", String.class));
    }

    @Test
    void claveInternaEsDeterministaYSeValida() {
        JwtService a = new JwtService(SECRETO, 5);
        JwtService b = new JwtService(SECRETO, 5);
        assertEquals(a.claveInterna(), b.claveInterna());
        assertTrue(a.esClaveInternaValida(b.claveInterna()));
        assertFalse(a.esClaveInternaValida("otra"));
        assertFalse(a.esClaveInternaValida(null));
    }

    @Test
    void rechazaSecretoCorto() {
        assertThrows(IllegalStateException.class, () -> new JwtService("corto", 5));
    }
}
