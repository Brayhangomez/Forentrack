package com.forentrack.laboratorio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.forentrack.laboratorio.model.Rol;
import com.forentrack.laboratorio.model.TipoAnalisis;

class TipoAnalisisTest {

    @Test
    void cadaTipoTieneUnUnicoRolResponsable() {
        assertEquals(TipoAnalisis.DIGITAL, TipoAnalisis.segunTipoEvidencia("DIGITAL"));
        assertEquals(TipoAnalisis.PERICIAL, TipoAnalisis.segunTipoEvidencia("FISICA"));
        assertEquals(TipoAnalisis.PERICIAL, TipoAnalisis.segunTipoEvidencia("DOCUMENTAL"));
        assertEquals(Rol.PERITO, TipoAnalisis.PERICIAL.getResponsable());
        assertEquals(Rol.ANALISTA_DIGITAL, TipoAnalisis.DIGITAL.getResponsable());
    }
}
