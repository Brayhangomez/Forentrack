package com.forentrack.custodia.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.forentrack.custodia.client.AuditoriaClient;

import jakarta.servlet.http.HttpServletRequest;

/** Traduce cualquier excepción a ErrorResponse con el código HTTP correcto (400/401/403/404/409/503). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditoriaClient auditor;

    public GlobalExceptionHandler(AuditoriaClient auditor) {
        this.auditor = auditor;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> api(ApiException ex) {
        return construir(ex.getStatus().value(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return construir(400, "Hay datos inválidos en la solicitud", errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoInvalido(HttpMessageNotReadableException ex) {
        return construir(400, "El cuerpo de la solicitud no es válido", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return construir(400, "El valor del parámetro '" + ex.getName() + "' no es válido", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> prohibido(AccessDeniedException ex, HttpServletRequest request) {
        auditor.denegado(request.getMethod(), request.getRequestURI());
        return construir(403, "No tiene permisos para esta operación", null);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> duplicado(DuplicateKeyException ex) {
        return construir(409, "Ya existe un registro con esos datos", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(NoResourceFoundException ex) {
        return construir(404, "Recurso no encontrado", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex) {
        // Errores propios de Spring MVC (405, 415, parámetros faltantes...) ya traen su código HTTP
        if (ex instanceof org.springframework.web.ErrorResponse conEstado) {
            int estado = conEstado.getStatusCode().value();
            HttpStatus status = HttpStatus.resolve(estado);
            return construir(estado, status != null ? status.getReasonPhrase() : "Solicitud no válida", null);
        }
        log.error("Error inesperado", ex);
        return construir(500, "Ocurrió un error interno", null);
    }

    private ResponseEntity<ErrorResponse> construir(int estado, String mensaje, Map<String, String> errores) {
        return ResponseEntity.status(estado)
                .body(new ErrorResponse(estado, mensaje, errores, LocalDateTime.now()));
    }
}
