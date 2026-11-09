package com.forentrack.web.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.forentrack.web.client.ApiClienteException;
import com.forentrack.web.client.AuditoriaClient;
import com.forentrack.web.client.CasosClient;
import com.forentrack.web.client.CustodiaClient;
import com.forentrack.web.client.EvidenciasClient;
import com.forentrack.web.client.LaboratorioClient;
import com.forentrack.web.client.UsuariosClient;
import com.forentrack.web.dto.CasoDto;
import com.forentrack.web.dto.EventoDto;
import com.forentrack.web.dto.EvidenciaDto;
import com.forentrack.web.dto.Indicador;
import com.forentrack.web.dto.ItemBandeja;
import com.forentrack.web.dto.MovimientoDto;
import com.forentrack.web.dto.ResumenDto;
import com.forentrack.web.dto.SolicitudDto;
import com.forentrack.web.dto.UsuarioDto;
import com.forentrack.web.model.Rol;

/** Arma el panel de inicio de cada rol: indicadores propios y bandeja de trabajo pendiente. */
@Service
public class PanelService {

    private static final int MAX_BANDEJA = 6;

    /** Contenido del panel; aviso no es null si algún servicio no respondió. */
    public record Panel(List<Indicador> indicadores, String tituloBandeja, List<ItemBandeja> bandeja, String aviso) {
    }

    private final UsuariosClient usuarios;
    private final CasosClient casos;
    private final EvidenciasClient evidencias;
    private final CustodiaClient custodia;
    private final LaboratorioClient laboratorio;
    private final AuditoriaClient auditoria;

    public PanelService(UsuariosClient usuarios, CasosClient casos, EvidenciasClient evidencias,
                        CustodiaClient custodia, LaboratorioClient laboratorio, AuditoriaClient auditoria) {
        this.usuarios = usuarios;
        this.casos = casos;
        this.evidencias = evidencias;
        this.custodia = custodia;
        this.laboratorio = laboratorio;
        this.auditoria = auditoria;
    }

    public Panel armar(Rol rol, String token, String correo) {
        Contexto c = new Contexto();
        switch (rol) {
            case ADMINISTRADOR -> administrador(c, token);
            case INVESTIGADOR -> investigador(c, token, correo);
            case FISCAL -> fiscal(c, token);
            case REVISOR_LEGAL -> revisorLegal(c, token);
            case TECNICO_CAMPO -> tecnico(c, token, correo);
            case CUSTODIO -> custodio(c, token);
            case PERITO, ANALISTA_DIGITAL -> analista(c, token, correo);
            case JEFE_LABORATORIO -> jefeLaboratorio(c, token);
            case AUDITOR -> auditor(c, token);
        }
        return new Panel(c.indicadores, c.titulo, c.bandeja.stream().limit(MAX_BANDEJA).toList(), c.aviso);
    }

    private void administrador(Contexto c, String token) {
        List<UsuarioDto> lista = c.cargar(() -> usuarios.listar(token, null));
        c.indicador("Usuarios", lista.size(), "bi-people", "azul");
        c.indicador("Activos", contar(lista, u -> Boolean.TRUE.equals(u.activo())), "bi-person-check", "verde");
        c.indicador("Inactivos", contar(lista, u -> !Boolean.TRUE.equals(u.activo())), "bi-person-x", "rojo");
        c.indicador("Roles en uso", lista.stream().map(UsuarioDto::rol).distinct().count(), "bi-diagram-3", "violeta");
        c.titulo = "Cuentas inactivas";
        lista.stream().filter(u -> !Boolean.TRUE.equals(u.activo())).forEach(u ->
                c.item(u.nombreCompleto(), u.correo(), u.rol(), "/usuarios"));
    }

    private void investigador(Contexto c, String token, String correo) {
        List<CasoDto> mios = c.cargar(() -> casos.buscar(token, null, null, correo));
        c.indicador("Casos asignados", mios.size(), "bi-folder2-open", "azul");
        c.indicador("Por iniciar", contarCasos(mios, "ASIGNADO"), "bi-play-circle", "indigo");
        c.indicador("En investigación", contarCasos(mios, "EN_INVESTIGACION"), "bi-search", "ambar");
        c.indicador("Suspendidos", contarCasos(mios, "SUSPENDIDO"), "bi-pause-circle", "naranja");
        c.titulo = "Mis casos activos";
        mios.stream().filter(k -> en(k.estado(), "ASIGNADO", "EN_INVESTIGACION")).forEach(k -> casoItem(c, k));
    }

    private void fiscal(Contexto c, String token) {
        List<CasoDto> todos = c.cargar(() -> casos.buscar(token, null, null, null));
        c.indicador("Por asignar", contarCasos(todos, "ABIERTO"), "bi-person-check", "azul");
        c.indicador("En investigación", contarCasos(todos, "EN_INVESTIGACION"), "bi-search", "ambar");
        c.indicador("Suspendidos", contarCasos(todos, "SUSPENDIDO"), "bi-pause-circle", "naranja");
        c.indicador("Listos para cierre", contar(todos, k -> "EN_INVESTIGACION".equals(k.estado())
                && "FAVORABLE".equals(k.conceptoLegal())), "bi-lock", "verde");
        c.titulo = "Casos que requieren tu decisión";
        todos.stream().filter(k -> "ABIERTO".equals(k.estado()) || ("EN_INVESTIGACION".equals(k.estado())
                && "FAVORABLE".equals(k.conceptoLegal()))).forEach(k -> casoItem(c, k));
    }

    private void revisorLegal(Contexto c, String token) {
        List<CasoDto> todos = c.cargar(() -> casos.buscar(token, null, null, null));
        List<EvidenciaDto> analizadas = c.cargar(() -> evidencias.listar(token, null, "ANALIZADA", null));
        Predicate<CasoDto> sinConcepto = k -> en(k.estado(), "EN_INVESTIGACION", "SUSPENDIDO") && k.conceptoLegal() == null;
        c.indicador("Casos sin concepto", contar(todos, sinConcepto), "bi-bank", "ambar");
        c.indicador("Conceptos favorables", contar(todos, k -> "FAVORABLE".equals(k.conceptoLegal())), "bi-check2-circle", "verde");
        c.indicador("Conceptos desfavorables", contar(todos, k -> "DESFAVORABLE".equals(k.conceptoLegal())), "bi-x-circle", "rojo");
        c.indicador("Evidencias analizadas", analizadas.size(), "bi-patch-check", "indigo");
        c.titulo = "Casos pendientes de concepto legal";
        todos.stream().filter(sinConcepto).forEach(k -> casoItem(c, k));
    }

    private void tecnico(Contexto c, String token, String correo) {
        List<EvidenciaDto> registradas = c.cargar(() -> evidencias.listar(token, null, "REGISTRADA", null));
        List<EvidenciaDto> mias = registradas.stream().filter(e -> correo != null && correo.equalsIgnoreCase(e.registradoPor())).toList();
        c.indicador("Registradas por mí", mias.size(), "bi-box-seam", "azul");
        c.indicador("Acta pendiente", contar(mias, e -> !Boolean.TRUE.equals(e.actaCompleta())), "bi-clipboard", "ambar");
        c.indicador("Listas para entregar", contar(mias, e -> Boolean.TRUE.equals(e.actaCompleta())), "bi-box-arrow-right", "verde");
        c.indicador("Sin custodia (todas)", registradas.size(), "bi-hourglass-split", "gris");
        c.titulo = "Mis evidencias por completar o entregar";
        mias.forEach(e -> c.item(e.codigo(), Boolean.TRUE.equals(e.actaCompleta()) ? "Lista para entregar a custodia"
                : "Falta el acta de recolección", e.estadoActual(), "/evidencias/" + e.id()));
    }

    private void custodio(Contexto c, String token) {
        List<MovimientoDto> pendientes = c.cargar(() -> custodia.pendientes(token));
        List<EvidenciaDto> todas = c.cargar(() -> evidencias.listar(token, null, null, null));
        c.indicador("Por recibir", pendientes.size(), "bi-box-arrow-in-down", "azul");
        c.indicador("En custodia", contar(todas, e -> "EN_CUSTODIA".equals(e.estadoActual())), "bi-safe", "indigo");
        c.indicador("En laboratorio", contar(todas, e -> "EN_ANALISIS".equals(e.estadoActual())), "bi-eyedropper", "ambar");
        c.indicador("Por devolver", contar(todas, e -> "ANALIZADA".equals(e.estadoActual())), "bi-box-arrow-up", "verde");
        c.titulo = "Entregas pendientes de recepción";
        pendientes.forEach(m -> c.item(m.evidenciaCodigo(), "Entregada por " + m.responsable(), m.tipo(),
                "/evidencias/" + m.evidenciaId()));
    }

    private void analista(Contexto c, String token, String correo) {
        List<SolicitudDto> mias = c.cargar(() -> laboratorio.listar(token, null, correo, null));
        c.indicador("Por iniciar", contarSolicitudes(mias, "ASIGNADA"), "bi-play-circle", "indigo");
        c.indicador("En proceso", contarSolicitudes(mias, "EN_PROCESO"), "bi-hourglass-split", "ambar");
        c.indicador("En revisión", contarSolicitudes(mias, "DICTAMINADA"), "bi-file-earmark-text", "violeta");
        c.indicador("Aprobadas", contarSolicitudes(mias, "APROBADA"), "bi-check2-circle", "verde");
        c.titulo = "Mis análisis pendientes";
        mias.stream().filter(s -> en(s.estado(), "ASIGNADA", "EN_PROCESO")).forEach(s -> solicitudItem(c, s));
    }

    private void jefeLaboratorio(Contexto c, String token) {
        List<SolicitudDto> todas = c.cargar(() -> laboratorio.listar(token, null, null, null));
        c.indicador("Por asignar", contarSolicitudes(todas, "SOLICITADA"), "bi-inbox", "azul");
        c.indicador("Asignadas", contarSolicitudes(todas, "ASIGNADA"), "bi-person-check", "indigo");
        c.indicador("En proceso", contarSolicitudes(todas, "EN_PROCESO"), "bi-hourglass-split", "ambar");
        c.indicador("Por revisar", contarSolicitudes(todas, "DICTAMINADA"), "bi-file-earmark-check", "violeta");
        c.titulo = "Solicitudes por asignar o revisar";
        todas.stream().filter(s -> en(s.estado(), "SOLICITADA", "DICTAMINADA")).forEach(s -> solicitudItem(c, s));
    }

    private void auditor(Contexto c, String token) {
        ResumenDto r = c.cargarUno(() -> auditoria.resumen(token));
        List<EventoDto> alertas = c.cargar(() -> auditoria.buscar(token,
                Map.of("resultado", "DENEGADO", "revisado", "false", "limite", "20")));
        c.indicador("Eventos registrados", r != null ? valor(r.total()) : 0, "bi-journal-text", "azul");
        c.indicador("Accesos denegados", r != null ? valor(r.denegados()) : 0, "bi-shield-x", "rojo");
        c.indicador("Operaciones fallidas", r != null ? valor(r.fallidos()) : 0, "bi-exclamation-triangle", "ambar");
        c.indicador("Alertas sin revisar", r != null ? valor(r.pendientesRevision()) : 0, "bi-flag", "violeta");
        c.titulo = "Accesos denegados sin revisar";
        alertas.forEach(e -> c.item(e.accion() + " · " + (e.usuario() != null ? e.usuario() : "anónimo"),
                e.recursoId(), e.resultado(), "/auditoria?resultado=DENEGADO&revisado=false"));
    }

    private static void casoItem(Contexto c, CasoDto k) {
        c.item(k.codigo(), k.titulo(), k.estado(), "/casos/" + k.id());
    }

    private static void solicitudItem(Contexto c, SolicitudDto s) {
        c.item(s.codigo() + " · " + s.evidenciaCodigo(), s.objetivo(), s.estado(), "/laboratorio");
    }

    private static long contarCasos(List<CasoDto> lista, String estado) {
        return contar(lista, k -> estado.equals(k.estado()));
    }

    private static long contarSolicitudes(List<SolicitudDto> lista, String estado) {
        return contar(lista, s -> estado.equals(s.estado()));
    }

    private static <T> long contar(List<T> lista, Predicate<T> condicion) {
        return lista.stream().filter(condicion).count();
    }

    private static long valor(Long numero) {
        return numero != null ? numero : 0;
    }

    private static boolean en(String valor, String... opciones) {
        return valor != null && List.of(opciones).contains(valor);
    }

    /** Acumula el contenido del panel y tolera servicios caídos (salvo el 401, que cierra la sesión). */
    private static final class Contexto {
        private final List<Indicador> indicadores = new ArrayList<>();
        private final List<ItemBandeja> bandeja = new ArrayList<>();
        private String titulo = "Pendientes";
        private String aviso;

        <T> List<T> cargar(Supplier<List<T>> consulta) {
            List<T> lista = cargarUno(consulta);
            return lista != null ? lista : List.of();
        }

        <T> T cargarUno(Supplier<T> consulta) {
            try {
                return consulta.get();
            } catch (ApiClienteException ex) {
                if (ex.getStatus() == 401) {
                    throw ex;
                }
                aviso = "Algunos indicadores no se pudieron cargar: " + ex.getMessage();
                return null;
            }
        }

        void indicador(String etiqueta, long valor, String icono, String tono) {
            indicadores.add(new Indicador(etiqueta, valor, icono, tono));
        }

        void item(String tituloItem, String detalle, String estado, String enlace) {
            bandeja.add(new ItemBandeja(tituloItem, detalle, estado, enlace));
        }
    }
}
