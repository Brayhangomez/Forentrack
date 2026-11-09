package com.forentrack.usuarios.dto;

import com.forentrack.usuarios.model.Rol;

/** Datos mínimos de un usuario activo para asignarle trabajo (sin id ni fechas). */
public record DirectorioResponse(String nombreCompleto, String correo, Rol rol) {
}
