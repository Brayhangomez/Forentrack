package com.forentrack.usuarios.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.forentrack.usuarios.client.AuditoriaClient;
import com.forentrack.usuarios.dto.LoginRequest;
import com.forentrack.usuarios.dto.LoginResponse;
import com.forentrack.usuarios.exception.ApiException;
import com.forentrack.usuarios.model.Usuario;
import com.forentrack.usuarios.repository.UsuarioRepository;
import com.forentrack.usuarios.security.JwtService;

@Service
public class AuthService {

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final AuditoriaClient auditoria;

    public AuthService(UsuarioRepository repositorio, PasswordEncoder encoder, JwtService jwtService,
                       AuditoriaClient auditoria) {
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.auditoria = auditoria;
    }

    public LoginResponse login(LoginRequest r) {
        String correo = r.correo().trim().toLowerCase();
        Usuario u = repositorio.findByCorreo(correo).orElse(null);
        // Mismo mensaje para cualquier fallo: no revela qué dato falló (HU-01)
        if (u == null || !u.isActivo() || u.getRol() == null
                || !encoder.matches(r.contrasena(), u.getContrasenaCifrada())) {
            auditoria.registrar("LOGIN", AuditoriaClient.FALLIDO, "USUARIO", null,
                    "Credenciales inválidas o cuenta inactiva", correo, null, null);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
        String token = jwtService.generar(u);
        auditoria.registrar("LOGIN", AuditoriaClient.EXITO, "USUARIO", u.getId(), "Inicio de sesión",
                u.getCorreo(), u.getRol().name(), "Bearer " + token);
        return new LoginResponse(token, "Bearer", jwtService.getExpiracionMinutos(),
                u.getNombreCompleto(), u.getRol(), u.getCorreo());
    }
}
