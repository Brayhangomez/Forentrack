package com.forentrack.custodia.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.forentrack.custodia.model.Rol;

/** Acceso cómodo al usuario autenticado y a su token para propagarlo a otros servicios. */
public final class UsuarioActual {

    private UsuarioActual() {
    }

    public static String correo() {
        Authentication a = autenticacion();
        return a != null ? a.getName() : null;
    }

    public static Rol rol() {
        Authentication a = autenticacion();
        if (a == null) {
            return null;
        }
        for (GrantedAuthority g : a.getAuthorities()) {
            String nombre = g.getAuthority();
            if (nombre != null && nombre.startsWith("ROLE_")) {
                return Rol.desde(nombre.substring(5));
            }
        }
        return null;
    }

    /** Cabecera Authorization original ("Bearer ...") de la petición en curso. */
    public static String authorization() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        }
        return null;
    }

    private static Authentication autenticacion() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a instanceof AnonymousAuthenticationToken || !a.isAuthenticated()) {
            return null;
        }
        return a;
    }
}
