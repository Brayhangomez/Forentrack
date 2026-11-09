package com.forentrack.gateway.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Prueba unitaria sin levantar el gateway. */
class ManejadorErroresGatewayTest {

    @Test
    void generaElMismoFormatoDeErrorQueLosServicios() {
        String json = ManejadorErroresGateway.json(503, "Servicio \"casos\" caído");
        assertTrue(json.startsWith("{\"estado\":503,\"mensaje\":\"Servicio 'casos' caído\""));
        assertTrue(json.contains("\"errores\":null"));
    }
}
