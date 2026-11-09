package com.forentrack.custodia.client;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.forentrack.custodia.exception.ApiException;
import com.forentrack.custodia.security.JwtService;
import com.forentrack.custodia.security.UsuarioActual;

/**
 * Cliente de evidencias-service (vía gateway). Propaga el JWT del usuario; los cambios de estado
 * llevan además la clave interna porque solo los servicios pueden pedirlos.
 */
@Component
public class EvidenciasClient {

    private final RestClient rest;
    private final JwtService jwtService;

    public EvidenciasClient(@Value("${gateway.api.url}") String gatewayUrl, JwtService jwtService) {
        this.rest = ClienteRest.crear(gatewayUrl, Duration.ofSeconds(30));
        this.jwtService = jwtService;
    }

    public EvidenciaRef obtener(String evidenciaId) {
        EvidenciaRef ref = ClienteRest.ejecutar("evidencias", () -> rest.get()
                .uri("/api/v1/evidencias/{id}", evidenciaId)
                .header(HttpHeaders.AUTHORIZATION, UsuarioActual.authorization())
                .retrieve()
                .body(EvidenciaRef.class));
        return requerido(ref);
    }

    public EvidenciaRef cambiarEstado(String evidenciaId, String estado, String observacion) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("estado", estado);
        cuerpo.put("observacion", observacion);
        EvidenciaRef ref = ClienteRest.ejecutar("evidencias", () -> rest.patch()
                .uri("/api/v1/evidencias/{id}/estado", evidenciaId)
                .header(HttpHeaders.AUTHORIZATION, UsuarioActual.authorization())
                .header(ClienteRest.CABECERA_INTERNA, jwtService.claveInterna())
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .body(EvidenciaRef.class));
        return requerido(ref);
    }

    private static EvidenciaRef requerido(EvidenciaRef ref) {
        if (ref == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de evidencias respondió vacío");
        }
        return ref;
    }

    /** Datos de la evidencia que necesita este servicio. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EvidenciaRef(String id, String codigo, String casoCodigo, String tipo,
                               String estadoActual, boolean actaCompleta) {
    }
}
