package com.forentrack.casos.client;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.forentrack.casos.security.UsuarioActual;

/** Consulta a usuarios-service (vía gateway) el directorio de investigadores, propagando el JWT del fiscal. */
@Component
public class UsuariosClient {

    private final RestClient rest;

    public UsuariosClient(@Value("${gateway.api.url}") String gatewayUrl) {
        this.rest = ClienteRest.crear(gatewayUrl, Duration.ofSeconds(30));
    }

    public List<UsuarioRef> investigadores() {
        List<UsuarioRef> lista = ClienteRest.ejecutar("usuarios", () -> rest.get()
                .uri("/api/v1/usuarios/directorio/investigadores")
                .header(HttpHeaders.AUTHORIZATION, UsuarioActual.authorization())
                .retrieve()
                .body(new ParameterizedTypeReference<List<UsuarioRef>>() { }));
        return lista != null ? lista : List.of();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UsuarioRef(String nombreCompleto, String correo, String rol) {
    }
}
