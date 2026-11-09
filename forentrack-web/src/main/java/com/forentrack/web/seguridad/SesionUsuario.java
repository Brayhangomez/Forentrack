package com.forentrack.web.seguridad;

import com.forentrack.web.dto.LoginResponseDto;
import com.forentrack.web.model.Rol;

import jakarta.servlet.http.HttpSession;

/** Datos del usuario guardados en la sesión HTTP de la web (el JWT nunca llega al navegador). */
public final class SesionUsuario {

    public static final String TOKEN = "token";
    public static final String NOMBRE = "nombre";
    public static final String ROL = "rol";
    public static final String CORREO = "correo";
    public static final String EXPIRA = "expiraEn";

    private SesionUsuario() {
    }

    public static void iniciar(HttpSession sesion, LoginResponseDto r) {
        long minutos = r.expiraEnMinutos() != null && r.expiraEnMinutos() > 0 ? r.expiraEnMinutos() : 120;
        sesion.setAttribute(TOKEN, r.token());
        sesion.setAttribute(NOMBRE, r.nombreCompleto());
        sesion.setAttribute(ROL, r.rol());
        sesion.setAttribute(CORREO, r.correo());
        // Margen de 30 s para no usar un token a punto de vencer
        sesion.setAttribute(EXPIRA, System.currentTimeMillis() + minutos * 60_000 - 30_000);
        sesion.setMaxInactiveInterval((int) Math.min(Integer.MAX_VALUE, minutos * 60));
    }

    public static String token(HttpSession sesion) {
        return sesion != null ? (String) sesion.getAttribute(TOKEN) : null;
    }

    public static Rol rol(HttpSession sesion) {
        return sesion != null ? Rol.desde((String) sesion.getAttribute(ROL)) : null;
    }

    public static String correo(HttpSession sesion) {
        return sesion != null ? (String) sesion.getAttribute(CORREO) : null;
    }

    public static String nombre(HttpSession sesion) {
        return sesion != null ? (String) sesion.getAttribute(NOMBRE) : null;
    }

    public static boolean expirada(HttpSession sesion) {
        Object expira = sesion.getAttribute(EXPIRA);
        return expira instanceof Long limite && System.currentTimeMillis() > limite;
    }
}
