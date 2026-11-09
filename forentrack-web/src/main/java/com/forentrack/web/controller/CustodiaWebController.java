package com.forentrack.web.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.forentrack.web.client.CustodiaClient;
import com.forentrack.web.model.Permiso;
import com.forentrack.web.seguridad.RequierePermiso;

import jakarta.servlet.http.HttpSession;

/** Vista general de la cadena de custodia (las acciones se hacen desde el detalle de cada evidencia). */
@Controller
public class CustodiaWebController extends ControladorBase {

    private static final List<String> TIPOS = List.of("ENTREGA", "RECEPCION", "TRASLADO", "DEVOLUCION");

    private final CustodiaClient custodiaClient;

    public CustodiaWebController(CustodiaClient custodiaClient) {
        this.custodiaClient = custodiaClient;
    }

    @GetMapping("/custodia")
    @RequierePermiso(Permiso.CUSTODIA_VER)
    public String inicio(@RequestParam(required = false) String tipo, HttpSession sesion, Model model) {
        String token = token(sesion);
        model.addAttribute("pendientes", opcional(model, () -> custodiaClient.pendientes(token), List.of()));
        model.addAttribute("movimientos", opcional(model, () -> custodiaClient.recientes(token, tipo), List.of()));
        model.addAttribute("tipoSel", tipo);
        model.addAttribute("tiposMovimiento", TIPOS);
        return "custodia";
    }
}
