package com.forentrack.usuarios.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.forentrack.usuarios.exception.ApiException;

/**
 * Utilidades comunes de los clientes REST: todos apuntan al API Gateway y traducen los errores
 * remotos a ApiException con el mismo código HTTP (o 503 si el servicio no responde).
 */
public final class ClienteRest {

    /** Cabecera con la clave interna para operaciones servicio a servicio. */
    public static final String CABECERA_INTERNA = "X-Internal-Key";

    private ClienteRest() {
    }

    public static RestClient crear(String baseUrl, Duration timeout) {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(timeout);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
    }

    public static <T> T ejecutar(String servicio, Supplier<T> operacion) {
        try {
            return operacion.get();
        } catch (RestClientResponseException ex) {
            int codigo = ex.getStatusCode().value();
            String mensaje = mensajeRemoto(ex);
            HttpStatus estado = switch (codigo) {
                case 400, 401, 403, 404, 409 -> HttpStatus.valueOf(codigo);
                default -> HttpStatus.SERVICE_UNAVAILABLE;
            };
            if (mensaje == null) {
                mensaje = estado == HttpStatus.SERVICE_UNAVAILABLE
                        ? "El servicio de " + servicio + " no está disponible"
                        : "El servicio de " + servicio + " rechazó la operación";
            }
            throw new ApiException(estado, mensaje);
        } catch (ResourceAccessException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de " + servicio + " no está disponible");
        }
    }

    private static String mensajeRemoto(RestClientResponseException ex) {
        try {
            ErrorRemoto error = ex.getResponseBodyAs(ErrorRemoto.class);
            return error != null ? error.mensaje() : null;
        } catch (RuntimeException ignorada) {
            return null; // cuerpo vacío o no JSON
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorRemoto(String mensaje) {
    }
}
