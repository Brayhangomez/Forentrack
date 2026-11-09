package com.forentrack.web.controller;

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.forentrack.web.client.ApiClienteException;
import com.forentrack.web.seguridad.SesionUsuario;

import jakarta.servlet.http.HttpSession;

/** Comportamiento común de los controladores web (evita repetir el manejo de errores en cada acción). */
public abstract class ControladorBase {

    protected static String token(HttpSession sesion) {
        return SesionUsuario.token(sesion);
    }

    /**
     * Ejecuta una acción de un botón o modal. Errores de negocio (400/404/409) se muestran como alerta;
     * 401, 403 y 503 los resuelve ModeloGlobal (login, mensaje de permisos o servicio caído).
     */
    protected String accion(RedirectAttributes flash, String destino, String mensajeOk, Runnable operacion) {
        try {
            operacion.run();
            flash.addFlashAttribute("ok", mensajeOk);
        } catch (ApiClienteException ex) {
            if (!ex.esNegocio()) {
                throw ex;
            }
            flash.addFlashAttribute("error", ex.getMessage());
            flash.addFlashAttribute("errores", ex.getErrores());
        }
        return "redirect:" + destino;
    }

    /** Consulta secundaria de una vista: si falla deja un aviso y devuelve el valor por defecto. */
    protected <T> T opcional(Model model, Supplier<T> consulta, T porDefecto) {
        try {
            T valor = consulta.get();
            return valor != null ? valor : porDefecto;
        } catch (ApiClienteException ex) {
            if (ex.getStatus() == 401) {
                throw ex;
            }
            model.addAttribute("aviso", "Parte de la información no se pudo cargar: " + ex.getMessage());
            return porDefecto;
        }
    }

    /** Pasa los errores por campo del microservicio al formulario; si no hay campo, queda como error general. */
    protected static void mostrarErrores(ApiClienteException ex, BindingResult resultado, Model model) {
        Map<String, String> errores = ex.getErrores();
        if (errores != null) {
            errores.forEach((campo, mensaje) -> {
                if (resultado.getFieldType(campo) != null) {
                    resultado.rejectValue(campo, "api", mensaje);
                }
            });
        }
        model.addAttribute("error", ex.getMessage());
    }

    protected static boolean en(String valor, String... opciones) {
        for (String o : opciones) {
            if (o.equals(valor)) {
                return true;
            }
        }
        return false;
    }
}
