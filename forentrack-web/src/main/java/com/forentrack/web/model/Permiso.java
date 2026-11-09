package com.forentrack.web.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Matriz Rol × Acción de la interfaz. Es el espejo de las reglas de seguridad de los microservicios
 * (SecurityFilterChain + @PreAuthorize): la web oculta lo que el backend igualmente rechazaría con 403.
 */
public enum Permiso {

    // ---- Consultas (visibles, pueden ser compartidas) ----
    CASO_VER("Consultar casos", "Buscar casos y ver su detalle", "bi-folder2-open", Rol.values()),
    EVIDENCIA_VER("Consultar evidencias", "Buscar evidencias y ver su historial", "bi-box-seam", Rol.values()),
    CUSTODIA_VER("Consultar cadena de custodia", "Ver movimientos y validaciones", "bi-link-45deg",
            todosMenos(Rol.ADMINISTRADOR)),
    LAB_VER("Consultar laboratorio", "Ver solicitudes de análisis y dictámenes", "bi-eyedropper",
            Rol.INVESTIGADOR, Rol.FISCAL, Rol.PERITO, Rol.ANALISTA_DIGITAL, Rol.JEFE_LABORATORIO,
            Rol.REVISOR_LEGAL, Rol.AUDITOR),

    // ---- ADMINISTRADOR ----
    USUARIO_VER("Listar usuarios", "Ver todas las cuentas y su rol", "bi-people", Rol.ADMINISTRADOR),
    USUARIO_CREAR("Crear usuario", "Crear cuentas y asignarles uno de los 10 roles", "bi-person-plus", Rol.ADMINISTRADOR),
    USUARIO_ESTADO("Activar / desactivar cuenta", "Bloquear o reactivar el acceso de un usuario", "bi-toggle-on",
            Rol.ADMINISTRADOR),

    // ---- INVESTIGADOR ----
    CASO_CREAR("Registrar caso", "Abrir un caso nuevo", "bi-folder-plus", Rol.INVESTIGADOR),
    CASO_INICIAR("Iniciar investigación", "Pasar un caso asignado a EN_INVESTIGACION", "bi-play-circle", Rol.INVESTIGADOR),
    CASO_NOTA("Agregar nota de investigación", "Documentar avances del caso", "bi-journal-plus", Rol.INVESTIGADOR),
    LAB_SOLICITAR("Solicitar análisis", "Pedir al laboratorio el análisis de una evidencia", "bi-send", Rol.INVESTIGADOR),

    // ---- FISCAL ----
    CASO_ASIGNAR("Asignar investigador", "Asignar o reasignar el investigador del caso", "bi-person-check", Rol.FISCAL),
    CASO_SUSPENDER("Suspender caso", "Detener temporalmente la investigación", "bi-pause-circle", Rol.FISCAL),
    CASO_REACTIVAR("Reactivar caso", "Retomar un caso suspendido", "bi-arrow-repeat", Rol.FISCAL),
    CASO_CERRAR("Cerrar caso", "Cerrar con concepto legal favorable", "bi-lock", Rol.FISCAL),

    // ---- REVISOR_LEGAL ----
    CASO_CONCEPTO("Emitir concepto legal", "Concepto FAVORABLE o DESFAVORABLE del caso", "bi-bank", Rol.REVISOR_LEGAL),
    CUSTODIA_VALIDAR("Validar cadena de custodia", "Declarar la cadena VÁLIDA u OBSERVADA", "bi-patch-check",
            Rol.REVISOR_LEGAL),
    EVIDENCIA_DESCARTAR("Autorizar descarte", "Descartar una evidencia con motivo legal", "bi-trash3", Rol.REVISOR_LEGAL),

    // ---- TECNICO_CAMPO ----
    EVIDENCIA_REGISTRAR("Registrar evidencia", "Registrar evidencia recolectada en un caso", "bi-plus-square",
            Rol.TECNICO_CAMPO),
    EVIDENCIA_ACTA("Completar acta de recolección", "Lugar, fecha, embalaje y observaciones", "bi-clipboard-check",
            Rol.TECNICO_CAMPO),
    CUSTODIA_ENTREGAR("Entregar a custodia", "Entregar la evidencia embalada a bodega", "bi-box-arrow-right",
            Rol.TECNICO_CAMPO),

    // ---- CUSTODIO ----
    CUSTODIA_RECIBIR("Recibir en bodega", "Ingresar la evidencia entregada (→ EN_CUSTODIA)", "bi-box-arrow-in-down",
            Rol.CUSTODIO),
    CUSTODIA_TRASLADAR("Trasladar evidencia", "Reubicar o trasladar la evidencia", "bi-truck", Rol.CUSTODIO),
    CUSTODIA_DEVOLVER("Devolver evidencia", "Devolución final de una evidencia analizada", "bi-box-arrow-up", Rol.CUSTODIO),

    // ---- PERITO ----
    LAB_PERITAJE_INICIAR("Iniciar peritaje", "Comenzar el análisis físico o documental", "bi-search", Rol.PERITO),
    LAB_DICTAMEN("Emitir dictamen pericial", "Registrar hallazgos y conclusiones periciales", "bi-file-earmark-text",
            Rol.PERITO),

    // ---- ANALISTA_DIGITAL ----
    LAB_DIGITAL_INICIAR("Iniciar análisis digital", "Comenzar el análisis de evidencia digital", "bi-cpu",
            Rol.ANALISTA_DIGITAL),
    LAB_HASH("Registrar hash SHA-256", "Huella de integridad de la imagen forense", "bi-fingerprint", Rol.ANALISTA_DIGITAL),
    LAB_INFORME("Emitir informe digital", "Informe técnico del análisis digital", "bi-file-earmark-binary",
            Rol.ANALISTA_DIGITAL),

    // ---- JEFE_LABORATORIO ----
    LAB_ASIGNAR("Asignar análisis", "Asignar solicitudes a peritos o analistas", "bi-diagram-3", Rol.JEFE_LABORATORIO),
    LAB_APROBAR("Aprobar dictamen", "Aprobar el dictamen (evidencia → ANALIZADA)", "bi-check2-circle",
            Rol.JEFE_LABORATORIO),
    LAB_RECHAZAR("Devolver dictamen", "Devolver el dictamen para corrección", "bi-arrow-counterclockwise",
            Rol.JEFE_LABORATORIO),

    // ---- AUDITOR ----
    AUDITORIA_VER("Consultar bitácora", "Revisar todas las acciones del sistema", "bi-shield-check", Rol.AUDITOR),
    AUDITORIA_EXPORTAR("Exportar bitácora", "Descargar la bitácora filtrada en CSV", "bi-filetype-csv", Rol.AUDITOR),
    AUDITORIA_REVISAR("Registrar hallazgo", "Marcar eventos como revisados con observación", "bi-flag", Rol.AUDITOR);

    private final String etiqueta;
    private final String descripcion;
    private final String icono;
    private final Set<Rol> roles;

    Permiso(String etiqueta, String descripcion, String icono, Rol... roles) {
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
        this.icono = icono;
        this.roles = roles.length == 0 ? EnumSet.noneOf(Rol.class) : EnumSet.copyOf(Arrays.asList(roles));
    }

    private static Rol[] todosMenos(Rol excluido) {
        return Arrays.stream(Rol.values()).filter(r -> r != excluido).toArray(Rol[]::new);
    }

    public boolean permite(Rol rol) {
        return rol != null && roles.contains(rol);
    }

    /** Una acción es exclusiva cuando solo un rol puede ejecutarla. */
    public boolean isExclusivo() {
        return roles.size() == 1;
    }

    public static List<Permiso> de(Rol rol) {
        return Arrays.stream(values()).filter(p -> p.permite(rol)).toList();
    }

    public static List<Permiso> exclusivosDe(Rol rol) {
        return Arrays.stream(values()).filter(p -> p.isExclusivo() && p.permite(rol)).toList();
    }

    public String getEtiqueta() { return etiqueta; }
    public String getDescripcion() { return descripcion; }
    public String getIcono() { return icono; }
    public Set<Rol> getRoles() { return roles; }
}
