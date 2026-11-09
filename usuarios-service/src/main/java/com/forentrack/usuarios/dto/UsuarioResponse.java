package com.forentrack.usuarios.dto;

import java.time.LocalDateTime;

import com.forentrack.usuarios.model.Rol;

public record UsuarioResponse(
        String id,
        String nombreCompleto,
        String correo,
        Rol rol,
        boolean activo,
        LocalDateTime fechaCreacion) {
}