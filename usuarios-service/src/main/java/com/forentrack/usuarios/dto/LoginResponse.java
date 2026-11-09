package com.forentrack.usuarios.dto;

import com.forentrack.usuarios.model.Rol;

/** Respuesta del login. El campo correo se añadió al final para no romper clientes anteriores. */
public record LoginResponse(
        String token,
        String tipo,
        long expiraEnMinutos,
        String nombreCompleto,
        Rol rol,
        String correo) {
}
