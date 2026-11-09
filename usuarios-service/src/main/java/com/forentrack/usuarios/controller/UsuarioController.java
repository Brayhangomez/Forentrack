package com.forentrack.usuarios.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.forentrack.usuarios.dto.DirectorioResponse;
import com.forentrack.usuarios.dto.EstadoRequest;
import com.forentrack.usuarios.dto.UsuarioRequest;
import com.forentrack.usuarios.dto.UsuarioResponse;
import com.forentrack.usuarios.model.Rol;
import com.forentrack.usuarios.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Gestión de cuentas (ADMINISTRADOR) y directorios para asignar trabajo")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Crear usuario con rol", description = "Rol exclusivo: ADMINISTRADOR")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario creado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos"),
        @ApiResponse(responseCode = "409", description = "Correo duplicado")
    })
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request) {
        return usuarioService.crear(request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Activar o desactivar una cuenta",
               description = "Rol exclusivo: ADMINISTRADOR. No borra el historial del usuario.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado actualizado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "409", description = "Intento de desactivar la propia cuenta")
    })
    public UsuarioResponse cambiarEstado(@PathVariable String id, @Valid @RequestBody EstadoRequest request) {
        return usuarioService.cambiarEstado(id, request.activo());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Listar usuarios", description = "Rol exclusivo: ADMINISTRADOR")
    public List<UsuarioResponse> listar(@RequestParam(required = false) Rol rol) {
        return usuarioService.listar(rol);
    }

    @GetMapping("/directorio/investigadores")
    @PreAuthorize("hasRole('FISCAL')")
    @Operation(summary = "Investigadores activos", description = "Rol: FISCAL (para asignar casos)")
    public List<DirectorioResponse> investigadores() {
        return usuarioService.investigadores();
    }

    @GetMapping("/directorio/laboratorio")
    @PreAuthorize("hasRole('JEFE_LABORATORIO')")
    @Operation(summary = "Peritos y analistas digitales activos",
               description = "Rol: JEFE_LABORATORIO (para asignar análisis)")
    public List<DirectorioResponse> laboratorio() {
        return usuarioService.laboratorio();
    }
}
