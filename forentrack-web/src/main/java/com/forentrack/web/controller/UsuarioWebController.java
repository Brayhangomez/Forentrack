package com.forentrack.web.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.forentrack.web.client.ApiClienteException;
import com.forentrack.web.client.UsuariosClient;
import com.forentrack.web.dto.UsuarioDto;
import com.forentrack.web.dto.UsuarioForm;
import com.forentrack.web.model.Permiso;
import com.forentrack.web.model.Rol;
import com.forentrack.web.seguridad.RequierePermiso;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/** Gestión de cuentas: exclusiva del ADMINISTRADOR. */
@Controller
public class UsuarioWebController extends ControladorBase {

    private final UsuariosClient usuariosClient;

    public UsuarioWebController(UsuariosClient usuariosClient) {
        this.usuariosClient = usuariosClient;
    }

    @ModelAttribute("roles")
    public List<Rol> roles() {
        return List.of(Rol.values());
    }

    /** Acciones exclusivas de cada rol para orientar al administrador al crear la cuenta. */
    @ModelAttribute("matriz")
    public Map<String, List<Permiso>> matriz() {
        Map<String, List<Permiso>> m = new LinkedHashMap<>();
        for (Rol r : Rol.values()) {
            m.put(r.name(), Permiso.exclusivosDe(r));
        }
        return m;
    }

    @GetMapping("/usuarios")
    @RequierePermiso(Permiso.USUARIO_VER)
    public String listar(@RequestParam(required = false) String rol, HttpSession sesion, Model model) {
        String filtro = Rol.desde(rol) != null ? rol : null;
        model.addAttribute("usuarios", opcional(model, () -> usuariosClient.listar(token(sesion), filtro), List.<UsuarioDto>of()));
        model.addAttribute("rolSel", filtro);
        return "usuarios";
    }

    @GetMapping("/usuarios/nuevo")
    @RequierePermiso(Permiso.USUARIO_CREAR)
    public String formulario(Model model) {
        model.addAttribute("form", new UsuarioForm());
        return "usuario-form";
    }

    @PostMapping("/usuarios")
    @RequierePermiso(Permiso.USUARIO_CREAR)
    public String crear(@Valid @ModelAttribute("form") UsuarioForm form, BindingResult resultado,
                        HttpSession sesion, Model model, RedirectAttributes flash) {
        if (resultado.hasErrors()) {
            return "usuario-form";
        }
        try {
            UsuarioDto creado = usuariosClient.crear(token(sesion), form);
            flash.addFlashAttribute("ok", "Usuario creado: " + creado.correo() + " (" + creado.rol() + ")");
            return "redirect:/usuarios";
        } catch (ApiClienteException ex) {
            if (!ex.esNegocio()) {
                throw ex;
            }
            mostrarErrores(ex, resultado, model);
            return "usuario-form";
        }
    }

    @PostMapping("/usuarios/{id}/estado")
    @RequierePermiso(Permiso.USUARIO_ESTADO)
    public String cambiarEstado(@PathVariable String id, @RequestParam boolean activo,
                                HttpSession sesion, RedirectAttributes flash) {
        return accion(flash, "/usuarios", activo ? "Cuenta activada" : "Cuenta desactivada",
                () -> usuariosClient.cambiarEstado(token(sesion), id, activo));
    }
}
