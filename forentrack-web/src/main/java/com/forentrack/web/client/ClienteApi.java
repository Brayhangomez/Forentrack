package com.forentrack.web.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.forentrack.web.dto.ErrorDto;

/**
 * Base de los clientes REST de la web. Todos llaman al API Gateway, envían el JWT de la sesión
 * y convierten cualquier fallo en ApiClienteException (503 si el servicio no responde).
 */
public abstract class ClienteApi {

    private final RestClient rest;
    private final String servicio;

    protected ClienteApi(String gatewayUrl, String servicio) {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        // Holgado porque en Render los servicios gratuitos tardan en despertar
        fabrica.setReadTimeout(Duration.ofSeconds(70));
        this.rest = RestClient.builder().baseUrl(gatewayUrl).requestFactory(fabrica).build();
        this.servicio = servicio;
    }

    protected <T> T consultar(String token, String ruta, Map<String, ?> params, Class<T> tipo, Object... vars) {
        return ejecutar(() -> pedirGet(token, ruta, params, vars).body(tipo));
    }

    protected <T> T consultarLista(String token, String ruta, Map<String, ?> params,
                                   ParameterizedTypeReference<T> tipo, Object... vars) {
        return ejecutar(() -> pedirGet(token, ruta, params, vars).body(tipo));
    }

    protected <T> T enviar(HttpMethod metodo, String token, String ruta, Object cuerpo, Class<T> tipo, Object... vars) {
        return ejecutar(() -> {
            RestClient.RequestBodySpec peticion = rest.method(metodo).uri(ruta, vars);
            if (token != null) {
                peticion = peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            if (cuerpo != null) {
                peticion = peticion.contentType(MediaType.APPLICATION_JSON).body(cuerpo);
            }
            return peticion.retrieve().body(tipo);
        });
    }

    /** Arma un mapa clave-valor que admite valores null (Map.of no los admite). */
    protected static Map<String, Object> mapa(Object... claveValor) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < claveValor.length; i += 2) {
            m.put((String) claveValor[i], claveValor[i + 1]);
        }
        return m;
    }

    private RestClient.ResponseSpec pedirGet(String token, String ruta, Map<String, ?> params, Object... vars) {
        return rest.get()
                .uri(u -> {
                    u.path(ruta);
                    if (params != null) {
                        params.forEach((clave, valor) -> {
                            if (valor != null && !valor.toString().isBlank()) {
                                u.queryParam(clave, valor.toString().trim());
                            }
                        });
                    }
                    return u.build(vars);
                })
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve();
    }

    private <T> T ejecutar(Supplier<T> operacion) {
        try {
            return operacion.get();
        } catch (RestClientResponseException ex) {
            ErrorDto error = null;
            try {
                error = ex.getResponseBodyAs(ErrorDto.class);
            } catch (RuntimeException ignorada) {
                // La respuesta puede venir vacía (por ejemplo, un 401)
            }
            String mensaje = error != null && error.mensaje() != null
                    ? error.mensaje() : "El servicio de " + servicio + " respondió con un error";
            throw new ApiClienteException(ex.getStatusCode().value(), mensaje, error != null ? error.errores() : null);
        } catch (ResourceAccessException ex) {
            throw new ApiClienteException(503, "No se pudo conectar con el servicio de " + servicio
                    + ". Intenta de nuevo en unos segundos.", null);
        } catch (RestClientException ex) {
            throw new ApiClienteException(503, "Respuesta inesperada del servicio de " + servicio, null);
        }
    }
}
