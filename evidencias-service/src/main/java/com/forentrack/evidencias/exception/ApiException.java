package com.forentrack.evidencias.exception;

import org.springframework.http.HttpStatus;

/** Error de negocio con su código HTTP; lo traduce GlobalExceptionHandler. */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
