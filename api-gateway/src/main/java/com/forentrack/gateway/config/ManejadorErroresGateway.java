package com.forentrack.gateway.config;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

import reactor.core.publisher.Mono;

/**
 * Devuelve los errores del gateway con el mismo JSON que los servicios: 404 si la ruta no existe
 * y 503 si el servicio destino no responde (caído, dormido en Render o con timeout).
 */
@Component
@Order(-2)
public class ManejadorErroresGateway implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErroresGateway.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse respuesta = exchange.getResponse();
        if (respuesta.isCommitted()) {
            return Mono.error(ex);
        }
        int estado = 503;
        String mensaje = "El servicio solicitado no está disponible en este momento";
        if (ex instanceof ResponseStatusException rse && rse.getStatusCode().value() < 500) {
            estado = rse.getStatusCode().value();
            mensaje = estado == 404 ? "Ruta no encontrada en el API Gateway" : "Solicitud no válida";
        } else {
            log.warn("Fallo al enrutar {}: {}", exchange.getRequest().getPath(), ex.toString());
        }
        byte[] cuerpo = json(estado, mensaje).getBytes(StandardCharsets.UTF_8);
        respuesta.setStatusCode(HttpStatusCode.valueOf(estado));
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return respuesta.writeWith(Mono.just(respuesta.bufferFactory().wrap(cuerpo)));
    }

    static String json(int estado, String mensaje) {
        return "{\"estado\":" + estado + ",\"mensaje\":\"" + mensaje.replace("\"", "'")
                + "\",\"errores\":null,\"fecha\":\"" + LocalDateTime.now() + "\"}";
    }
}
