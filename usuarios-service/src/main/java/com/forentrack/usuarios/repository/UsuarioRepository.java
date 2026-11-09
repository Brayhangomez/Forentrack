package com.forentrack.usuarios.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.forentrack.usuarios.model.Rol;
import com.forentrack.usuarios.model.Usuario;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    List<Usuario> findByRol(Rol rol, Sort sort);

    List<Usuario> findByRolInAndActivoTrue(Collection<Rol> roles, Sort sort);
}
