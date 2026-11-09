package com.forentrack.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.forentrack.web.model.Permiso;
import com.forentrack.web.model.Rol;

/** Verifica las reglas de la matriz Rol × Acción sin levantar Spring. */
class MatrizPermisosTest {

    @Test
    void hayDiezRoles() {
        assertEquals(10, Rol.values().length);
    }

    @Test
    void cadaRolTieneAlMenosDosAccionesExclusivasYCuatroVisibles() {
        for (Rol rol : Rol.values()) {
            assertTrue(Permiso.exclusivosDe(rol).size() >= 2, rol + " necesita 2 acciones exclusivas");
            assertTrue(Permiso.de(rol).size() >= 4, rol + " necesita 4 acciones visibles");
        }
    }

    @Test
    void ningunaAccionExclusivaSeRepiteEntreRoles() {
        Set<Permiso> vistos = EnumSet.noneOf(Permiso.class);
        for (Rol rol : Rol.values()) {
            for (Permiso p : Permiso.exclusivosDe(rol)) {
                assertTrue(vistos.add(p), p + " aparece en más de un rol");
            }
        }
    }

    @Test
    void lasAccionesQueModificanDatosSonExclusivas() {
        Set<Permiso> consultas = EnumSet.of(Permiso.CASO_VER, Permiso.EVIDENCIA_VER, Permiso.CUSTODIA_VER, Permiso.LAB_VER);
        for (Permiso p : Permiso.values()) {
            if (!consultas.contains(p)) {
                assertTrue(p.isExclusivo(), p + " debería tener un único rol");
            }
        }
    }
}
