package com.forentrack.evidencias.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.forentrack.evidencias.dto.CasoRef;
import com.forentrack.evidencias.exception.ApiException;
import com.forentrack.evidencias.security.UsuarioActual;

/** Consulta casos-service a través del gateway propagando el JWT del usuario. */
@Component
public class CasosClient {

    private final RestClient rest;

    public CasosClient(@Value("${gateway.api.url}") String gatewayUrl) {
        this.rest = ClienteRest.crear(gatewayUrl, Duration.ofSeconds(30));
    }

    public CasoRef obtener(String casoId) {
        try {
            CasoRef caso = ClienteRest.ejecutar("casos", () -> rest.get()
                    .uri("/api/v1/casos/{id}", casoId)
                    .header(HttpHeaders.AUTHORIZATION, UsuarioActual.authorization())
                    .retrieve()
                    .body(CasoRef.class));
            if (caso == null) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de casos respondió vacío");
            }
            return caso;
        } catch (ApiException ex) {
            if (ex.getStatus() == HttpStatus.NOT_FOUND) {
                throw new ApiException(HttpStatus.NOT_FOUND, "El caso indicado no existe");
            }
            throw ex;
        }
    }
}
