package com.forentrack.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.forentrack.web.model.Permiso;
import com.forentrack.web.model.Rol;
import com.forentrack.web.seguridad.SesionUsuario;
import com.forentrack.web.service.PanelService;

import jakarta.servlet.http.HttpSession;

@Controller
public class PanelController extends ControladorBase {

    private final PanelService panelService;

    public PanelController(PanelService panelService) {
        this.panelService = panelService;
    }

    @GetMapping("/panel")
    public String panel(HttpSession sesion, Model model) {
        Rol rol = SesionUsuario.rol(sesion);
        model.addAttribute("panel", panelService.armar(rol, token(sesion), SesionUsuario.correo(sesion)));
        model.addAttribute("exclusivas", Permiso.exclusivosDe(rol));
        model.addAttribute("compartidas", Permiso.de(rol).stream().filter(p -> !p.isExclusivo()).toList());
        return "panel";
    }
}
