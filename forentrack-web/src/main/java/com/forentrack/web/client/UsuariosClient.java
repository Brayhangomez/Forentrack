package com.forentrack.web.client;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.forentrack.web.dto.DirectorioDto;
import com.forentrack.web.dto.LoginResponseDto;
import com.forentrack.web.dto.UsuarioDto;
import com.forentrack.web.dto.UsuarioForm;

@Component
public class UsuariosClient extends ClienteApi {

    private static final ParameterizedTypeReference<List<UsuarioDto>> LISTA = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<DirectorioDto>> DIRECTORIO = new ParameterizedTypeReference<>() { };

    public UsuariosClient(@Value("${gateway.api.url}") String gatewayUrl) {
        super(gatewayUrl, "usuarios");
    }

    public LoginResponseDto login(String correo, String contrasena) {
        return enviar(HttpMethod.POST, null, "/api/v1/auth/login",
                mapa("correo", correo, "contrasena", contrasena), LoginResponseDto.class);
    }

    public UsuarioDto crear(String token, UsuarioForm f) {
        return enviar(HttpMethod.POST, token, "/api/v1/usuarios", mapa("nombreCompleto", f.getNombreCompleto(),
                "correo", f.getCorreo(), "contrasena", f.getContrasena(), "rol", f.getRol()), UsuarioDto.class);
    }

    public List<UsuarioDto> listar(String token, String rol) {
        return consultarLista(token, "/api/v1/usuarios", Map.of("rol", rol == null ? "" : rol), LISTA);
    }

    public UsuarioDto cambiarEstado(String token, String id, boolean activo) {
        return enviar(HttpMethod.PATCH, token, "/api/v1/usuarios/{id}/estado", mapa("activo", activo), UsuarioDto.class, id);
    }

    public List<DirectorioDto> investigadores(String token) {
        return consultarLista(token, "/api/v1/usuarios/directorio/investigadores", null, DIRECTORIO);
    }

    public List<DirectorioDto> personalLaboratorio(String token) {
        return consultarLista(token, "/api/v1/usuarios/directorio/laboratorio", null, DIRECTORIO);
    }
}
