package com.forentrack.usuarios.service;

import java.util.EnumSet;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.forentrack.usuarios.client.AuditoriaClient;
import com.forentrack.usuarios.dto.DirectorioResponse;
import com.forentrack.usuarios.dto.UsuarioRequest;
import com.forentrack.usuarios.dto.UsuarioResponse;
import com.forentrack.usuarios.exception.ApiException;
import com.forentrack.usuarios.model.Rol;
import com.forentrack.usuarios.model.Usuario;
import com.forentrack.usuarios.repository.UsuarioRepository;
import com.forentrack.usuarios.security.UsuarioActual;

@Service
public class UsuarioService {

    private static final Sort POR_NOMBRE = Sort.by("nombreCompleto");

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;
    private final AuditoriaClient auditoria;

    public UsuarioService(UsuarioRepository repositorio, PasswordEncoder encoder, AuditoriaClient auditoria) {
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.auditoria = auditoria;
    }

    /** ADMINISTRADOR: crear usuario con rol. */
    public UsuarioResponse crear(UsuarioRequest r) {
        String correo = r.correo().trim().toLowerCase();
        if (repositorio.existsByCorreo(correo)) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
        Usuario u = new Usuario();
        u.setNombreCompleto(r.nombreCompleto().trim());
        u.setCorreo(correo);
        u.setContrasenaCifrada(encoder.encode(r.contrasena()));
        u.setRol(r.rol());
        Usuario guardado = repositorio.save(u);
        auditoria.exito("USUARIO_CREADO", "USUARIO", guardado.getId(), correo + " con rol " + r.rol());
        return aRespuesta(guardado);
    }

    /** ADMINISTRADOR: activar o desactivar una cuenta (no puede desactivarse a sí mismo). */
    public UsuarioResponse cambiarEstado(String id, boolean activo) {
        Usuario u = repositorio.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (!activo && u.getCorreo().equals(UsuarioActual.correo())) {
            throw new ApiException(HttpStatus.CONFLICT, "No puede desactivar su propia cuenta");
        }
        u.setActivo(activo);
        Usuario guardado = repositorio.save(u);
        auditoria.exito(activo ? "USUARIO_ACTIVADO" : "USUARIO_DESACTIVADO", "USUARIO", id, u.getCorreo());
        return aRespuesta(guardado);
    }

    /** ADMINISTRADOR: listar usuarios, opcionalmente por rol. */
    public List<UsuarioResponse> listar(Rol rol) {
        List<Usuario> lista = rol != null ? repositorio.findByRol(rol, POR_NOMBRE) : repositorio.findAll(POR_NOMBRE);
        return lista.stream().map(this::aRespuesta).toList();
    }

    /** FISCAL: investigadores activos a los que puede asignar casos. */
    public List<DirectorioResponse> investigadores() {
        return directorio(EnumSet.of(Rol.INVESTIGADOR));
    }

    /** JEFE_LABORATORIO: peritos y analistas digitales activos a los que puede asignar análisis. */
    public List<DirectorioResponse> laboratorio() {
        return directorio(EnumSet.of(Rol.PERITO, Rol.ANALISTA_DIGITAL));
    }

    private List<DirectorioResponse> directorio(EnumSet<Rol> roles) {
        return repositorio.findByRolInAndActivoTrue(roles, POR_NOMBRE).stream()
                .map(u -> new DirectorioResponse(u.getNombreCompleto(), u.getCorreo(), u.getRol()))
                .toList();
    }

    private UsuarioResponse aRespuesta(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombreCompleto(), u.getCorreo(),
                u.getRol(), u.isActivo(), u.getFechaCreacion());
    }
}
