package com.forentrack.web.client;

import java.util.Map;

/** Error devuelto por un microservicio (código HTTP, mensaje y errores por campo). */
public class ApiClienteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final transient Map<String, String> errores;

    public ApiClienteException(int status, String mensaje, Map<String, String> errores) {
        super(mensaje);
        this.status = status;
        this.errores = errores;
    }

    public int getStatus() { return status; }
    public Map<String, String> getErrores() { return errores; }

    /** 400, 404 y 409 son errores de negocio que se muestran en el formulario o la vista actual. */
    public boolean esNegocio() {
        return status == 400 || status == 404 || status == 409;
    }
}
