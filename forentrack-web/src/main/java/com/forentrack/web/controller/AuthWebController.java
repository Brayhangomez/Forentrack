package com.forentrack.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.forentrack.web.client.ApiClienteException;
import com.forentrack.web.client.UsuariosClient;
import com.forentrack.web.dto.LoginForm;
import com.forentrack.web.dto.LoginResponseDto;
import com.forentrack.web.model.Rol;
import com.forentrack.web.seguridad.SesionUsuario;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class AuthWebController {

    private final UsuariosClient usuariosClient;

    public AuthWebController(UsuariosClient usuariosClient) {
        this.usuariosClient = usuariosClient;
    }

    @GetMapping("/")
    public String raiz() {
        return "redirect:/panel";
    }

    @GetMapping("/login")
    public String mostrarLogin(Model model, HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null && SesionUsuario.token(sesion) != null && !SesionUsuario.expirada(sesion)) {
            return "redirect:/panel";
        }
        model.addAttribute("form", new LoginForm());
        return "login";
    }

    @PostMapping("/login")
    public String iniciarSesion(@Valid @ModelAttribute("form") LoginForm form, BindingResult resultado,
                                HttpServletRequest request, Model model) {
        if (resultado.hasErrors()) {
            return "login";
        }
        try {
            LoginResponseDto respuesta = usuariosClient.login(form.getCorreo().trim(), form.getContrasena());
            if (respuesta == null || respuesta.token() == null || Rol.desde(respuesta.rol()) == null) {
                model.addAttribute("error", "La respuesta del servicio de usuarios no es válida");
                return "login";
            }
            HttpSession sesion = request.getSession();
            request.changeSessionId(); // evita la fijación de sesión
            SesionUsuario.iniciar(sesion, respuesta);
            return "redirect:/panel";
        } catch (ApiClienteException ex) {
            model.addAttribute("error", ex.getStatus() == 401 ? "Credenciales inválidas" : ex.getMessage());
            return "login";
        }
    }

    @PostMapping("/logout")
    public String cerrarSesion(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        return "redirect:/login?salio";
    }
}
