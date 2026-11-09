package com.forentrack.web.config;

import java.io.IOException;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.forentrack.web.model.Rol;
import com.forentrack.web.seguridad.RequierePermiso;
import com.forentrack.web.seguridad.SesionUsuario;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Exige sesión iniciada, cierra la sesión cuando el JWT venció (redirige a /login?expiro)
 * y responde 403 real si el rol no tiene el permiso que declara el controlador.
 */
public class SesionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws IOException {
        HttpSession sesion = request.getSession(false);
        if (sesion == null || SesionUsuario.token(sesion) == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        Rol rol = SesionUsuario.rol(sesion);
        if (SesionUsuario.expirada(sesion) || rol == null) {
            sesion.invalidate();
            response.sendRedirect(request.getContextPath() + "/login?expiro");
            return false;
        }
        if (handler instanceof HandlerMethod metodo) {
            RequierePermiso requerido = metodo.getMethodAnnotation(RequierePermiso.class);
            if (requerido != null && !requerido.value().permite(rol)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tu rol no tiene acceso a esta opción");
                return false;
            }
        }
        return true;
    }
}
