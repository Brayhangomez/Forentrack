package com.forentrack.usuarios.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.forentrack.usuarios.model.Rol;
import com.forentrack.usuarios.model.Usuario;
import com.forentrack.usuarios.repository.UsuarioRepository;

/**
 * Crea un usuario de prueba por cada uno de los 10 roles si su correo aún no existe
 * (nunca modifica usuarios existentes). Las contraseñas salen de variables de entorno;
 * los valores por defecto del application.properties son solo para desarrollo.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    /** Nombre visible y parte local del correo de cada usuario de prueba. */
    private static final Map<Rol, String[]> PRUEBA = Map.of(
            Rol.ADMINISTRADOR, new String[] {"Administrador de prueba", "admin"},
            Rol.INVESTIGADOR, new String[] {"Investigador de prueba", "investigador"},
            Rol.PERITO, new String[] {"Perito de prueba", "perito"},
            Rol.CUSTODIO, new String[] {"Custodio de prueba", "custodio"},
            Rol.FISCAL, new String[] {"Fiscal de prueba", "fiscal"},
            Rol.AUDITOR, new String[] {"Auditor de prueba", "auditor"},
            Rol.TECNICO_CAMPO, new String[] {"Técnico de campo de prueba", "tecnico"},
            Rol.ANALISTA_DIGITAL, new String[] {"Analista digital de prueba", "analista"},
            Rol.JEFE_LABORATORIO, new String[] {"Jefe de laboratorio de prueba", "jefelab"},
            Rol.REVISOR_LEGAL, new String[] {"Revisor legal de prueba", "legal"});

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;
    private final Environment entorno;
    private final boolean habilitado;
    private final String dominio;
    private final String correoAdmin;

    public DatosIniciales(UsuarioRepository repositorio, PasswordEncoder encoder, Environment entorno,
                          @Value("${app.seed.enabled:true}") boolean habilitado,
                          @Value("${app.seed.dominio:forentrack.test}") String dominio,
                          @Value("${app.admin.correo:admin@forentrack.test}") String correoAdmin) {
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.entorno = entorno;
        this.habilitado = habilitado;
        this.dominio = dominio;
        this.correoAdmin = correoAdmin;
    }

    @Override
    public void run(String... args) {
        if (!habilitado) {
            log.info("SEED_ENABLED=false: no se crean usuarios de prueba");
            return;
        }
        for (Rol rol : Rol.values()) {
            String[] datos = PRUEBA.get(rol);
            String correo = rol == Rol.ADMINISTRADOR ? correoAdmin : datos[1] + "@" + dominio;
            String contrasena = entorno.getProperty("app.seed.passwords." + rol.name(), "");
            crearSiNoExiste(datos[0], correo.trim().toLowerCase(), contrasena, rol);
        }
    }

    private void crearSiNoExiste(String nombre, String correo, String contrasena, Rol rol) {
        if (contrasena.isBlank() || contrasena.length() < 8) {
            log.warn("Contraseña de prueba para {} vacía o menor a 8 caracteres: no se crea {}", rol, correo);
            return;
        }
        if (repositorio.existsByCorreo(correo)) {
            return;
        }
        Usuario u = new Usuario();
        u.setNombreCompleto(nombre);
        u.setCorreo(correo);
        u.setContrasenaCifrada(encoder.encode(contrasena));
        u.setRol(rol);
        repositorio.save(u);
        log.info("Usuario de prueba creado: {} ({})", correo, rol);
    }
}
