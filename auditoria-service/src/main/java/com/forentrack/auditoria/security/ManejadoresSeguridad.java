package com.forentrack.auditoria.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.forentrack.auditoria.service.BitacoraService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Respuestas JSON para 401 (sin token o token expirado) y 403 (rol sin permiso en la ruta).
 * Los 403 quedan registrados en la bitácora de auditoría.
 */
@Component
public class ManejadoresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final BitacoraService auditor;

    public ManejadoresSeguridad(BitacoraService auditor) {
        this.auditor = auditor;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        escribir(response, 401, "Debe iniciar sesión o su token expiró");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        auditor.denegado(request.getMethod(), request.getRequestURI());
        escribir(response, 403, "No tiene permisos para esta operación");
    }

    private static void escribir(HttpServletResponse response, int estado, String mensaje) throws IOException {
        response.setStatus(estado);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"estado\":" + estado + ",\"mensaje\":\"" + mensaje
                + "\",\"errores\":null,\"fecha\":\"" + LocalDateTime.now() + "\"}");
    }
}
