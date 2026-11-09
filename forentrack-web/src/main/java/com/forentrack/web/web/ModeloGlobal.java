package com.forentrack.web.web;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.forentrack.web.client.ApiClienteException;
import com.forentrack.web.model.Permiso;
import com.forentrack.web.model.Rol;
import com.forentrack.web.seguridad.SesionUsuario;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Datos comunes a todas las vistas (usuario, permisos, menú por rol) y manejo central de errores
 * remotos: 401 cierra la sesión y lleva al login; 403/503 muestran un mensaje sin romper la vista.
 */
@ControllerAdvice
public class ModeloGlobal {

    /** Elemento del menú lateral; se muestra solo si el rol tiene el permiso (null = todos). */
    public record ItemMenu(String clave, String etiqueta, String icono, String enlace, Permiso permiso, String grupo) {
    }

    /** Datos del usuario de la sesión para las plantillas. */
    public record Yo(String nombre, String correo, String rol, String rolEtiqueta) {
    }

    private static final List<ItemMenu> MENU = List.of(
            new ItemMenu("panel", "Panel", "bi-grid-1x2", "/panel", null, "General"),
            new ItemMenu("casos", "Casos", "bi-folder2-open", "/casos", Permiso.CASO_VER, "Operación"),
            new ItemMenu("evidencias", "Evidencias", "bi-box-seam", "/evidencias", Permiso.EVIDENCIA_VER, "Operación"),
            new ItemMenu("custodia", "Cadena de custodia", "bi-link-45deg", "/custodia", Permiso.CUSTODIA_VER, "Operación"),
            new ItemMenu("laboratorio", "Laboratorio", "bi-eyedropper", "/laboratorio", Permiso.LAB_VER, "Operación"),
            new ItemMenu("auditoria", "Auditoría", "bi-shield-check", "/auditoria", Permiso.AUDITORIA_VER, "Control"),
            new ItemMenu("usuarios", "Usuarios", "bi-people", "/usuarios", Permiso.USUARIO_VER, "Control"));

    private static final List<ItemMenu> RAPIDAS = List.of(
            new ItemMenu("nuevo-caso", "Registrar caso", "bi-folder-plus", "/casos/nuevo", Permiso.CASO_CREAR, null),
            new ItemMenu("nueva-evidencia", "Registrar evidencia", "bi-plus-square", "/evidencias/nuevo",
                    Permiso.EVIDENCIA_REGISTRAR, null),
            new ItemMenu("nuevo-usuario", "Crear usuario", "bi-person-plus", "/usuarios/nuevo", Permiso.USUARIO_CREAR, null));

    @ModelAttribute("yo")
    public Yo yo(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        Rol rol = SesionUsuario.rol(sesion);
        if (rol == null) {
            return null;
        }
        return new Yo(SesionUsuario.nombre(sesion), SesionUsuario.correo(sesion), rol.name(), rol.getEtiqueta());
    }

    /** Nombres de los permisos del rol: en las plantillas ${puede.contains('CASO_CREAR')}. */
    @ModelAttribute("puede")
    public Set<String> puede(HttpServletRequest request) {
        Rol rol = SesionUsuario.rol(request.getSession(false));
        return Permiso.de(rol).stream().map(Enum::name).collect(Collectors.toUnmodifiableSet());
    }

    @ModelAttribute("menu")
    public List<ItemMenu> menu(HttpServletRequest request) {
        return filtrar(MENU, SesionUsuario.rol(request.getSession(false)));
    }

    @ModelAttribute("rapidas")
    public List<ItemMenu> rapidas(HttpServletRequest request) {
        return filtrar(RAPIDAS, SesionUsuario.rol(request.getSession(false)));
    }

    private static List<ItemMenu> filtrar(List<ItemMenu> items, Rol rol) {
        if (rol == null) {
            return List.of();
        }
        return items.stream().filter(i -> i.permiso() == null || i.permiso().permite(rol)).toList();
    }

    @ExceptionHandler(ApiClienteException.class)
    public ModelAndView errorRemoto(ApiClienteException ex, HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (ex.getStatus() == 401) {
            if (sesion != null) {
                sesion.invalidate();
            }
            return new ModelAndView("redirect:/login?expiro");
        }
        String mensaje = ex.getStatus() == 403
                ? "Tu rol no tiene permiso para realizar esta acción." : ex.getMessage();
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            ModelAndView vista = new ModelAndView("error");
            HttpStatus estado = HttpStatus.resolve(ex.getStatus());
            vista.setStatus(estado != null ? estado : HttpStatus.SERVICE_UNAVAILABLE);
            vista.addObject("status", ex.getStatus());
            vista.addObject("mensaje", mensaje);
            return vista;
        }
        RequestContextUtils.getOutputFlashMap(request).put("error", mensaje);
        return new ModelAndView("redirect:" + destinoSeguro(request));
    }

    /** Vuelve a la página anterior (solo rutas internas) para no abrir redirecciones externas. */
    private static String destinoSeguro(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                URI uri = URI.create(referer);
                String ruta = uri.getRawPath();
                if (ruta != null && ruta.startsWith("/") && !ruta.startsWith("//")) {
                    return uri.getRawQuery() != null ? ruta + "?" + uri.getRawQuery() : ruta;
                }
            } catch (IllegalArgumentException ignorada) {
                // Referer mal formado: se usa el panel
            }
        }
        return "/panel";
    }
}
