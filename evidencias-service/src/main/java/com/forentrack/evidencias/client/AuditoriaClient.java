package com.forentrack.evidencias.client;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.forentrack.evidencias.model.Rol;
import com.forentrack.evidencias.security.JwtService;
import com.forentrack.evidencias.security.UsuarioActual;

/**
 * Envía cada acción a auditoria-service (a través del gateway) propagando el JWT del usuario.
 * El envío es asíncrono y nunca lanza excepciones: la auditoría no debe frenar la operación.
 */
@Component
public class AuditoriaClient {

    public static final String EXITO = "EXITO";
    public static final String FALLIDO = "FALLIDO";
    public static final String DENEGADO = "DENEGADO";

    private static final Logger log = LoggerFactory.getLogger(AuditoriaClient.class);

    private final RestClient rest;
    private final JwtService jwtService;
    private final String servicio;

    public AuditoriaClient(@Value("${gateway.api.url}") String gatewayUrl,
                           @Value("${spring.application.name}") String servicio,
                           JwtService jwtService) {
        this.rest = ClienteRest.crear(gatewayUrl, Duration.ofSeconds(5));
        this.jwtService = jwtService;
        this.servicio = servicio;
    }

    /** Acción exitosa del usuario autenticado. */
    public void exito(String accion, String recursoTipo, String recursoId, String detalle) {
        Rol rol = UsuarioActual.rol();
        registrar(accion, EXITO, recursoTipo, recursoId, detalle,
                UsuarioActual.correo(), rol != null ? rol.name() : null, UsuarioActual.authorization());
    }

    /** Intento de acceso a una ruta o acción no permitida para el rol (403). */
    public void denegado(String metodo, String ruta) {
        Rol rol = UsuarioActual.rol();
        registrar("ACCESO_DENEGADO", DENEGADO, "RUTA", metodo + " " + ruta, "Intento sin permiso",
                UsuarioActual.correo(), rol != null ? rol.name() : null, UsuarioActual.authorization());
    }

    public void registrar(String accion, String resultado, String recursoTipo, String recursoId, String detalle,
                          String usuario, String rol, String authorization) {
        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("servicio", servicio);
        evento.put("accion", accion);
        evento.put("resultado", resultado);
        // Se recorta a los límites que valida auditoria-service para que el evento nunca sea rechazado
        evento.put("usuario", recortar(usuario, 160));
        evento.put("rol", rol);
        evento.put("recursoTipo", recortar(recursoTipo, 60));
        evento.put("recursoId", recortar(recursoId, 200));
        evento.put("detalle", recortar(detalle, 1000));
        // Los datos se capturan antes porque el hilo virtual ya no tiene acceso a la petición
        Thread.ofVirtual().start(() -> enviar(evento, authorization));
    }

    private static String recortar(String texto, int maximo) {
        return texto != null && texto.length() > maximo ? texto.substring(0, maximo) : texto;
    }

    private void enviar(Map<String, Object> evento, String authorization) {
        try {
            RestClient.RequestBodySpec peticion = rest.post()
                    .uri("/api/v1/auditoria/eventos")
                    .header(ClienteRest.CABECERA_INTERNA, jwtService.claveInterna())
                    .contentType(MediaType.APPLICATION_JSON);
            if (authorization != null) {
                peticion = peticion.header(HttpHeaders.AUTHORIZATION, authorization);
            }
            peticion.body(evento).retrieve().toBodilessEntity();
        } catch (RuntimeException ex) {
            log.warn("No se pudo registrar el evento de auditoría {}: {}", evento.get("accion"), ex.getMessage());
        }
    }
}
