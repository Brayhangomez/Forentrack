package com.forentrack.casos.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.forentrack.casos.client.AuditoriaClient;
import com.forentrack.casos.client.UsuariosClient;
import com.forentrack.casos.dto.CasoRequest;
import com.forentrack.casos.dto.CasoResponse;
import com.forentrack.casos.exception.ApiException;
import com.forentrack.casos.model.CambioEstado;
import com.forentrack.casos.model.Caso;
import com.forentrack.casos.model.ConceptoLegal;
import com.forentrack.casos.model.EstadoCaso;
import com.forentrack.casos.model.Nota;
import com.forentrack.casos.model.Rol;
import com.forentrack.casos.repository.CasoRepository;
import com.forentrack.casos.security.UsuarioActual;

/** Reglas del ciclo de vida del caso. Cada método corresponde a una acción exclusiva de un rol. */
@Service
public class CasoService {

    private static final String RECURSO = "CASO";

    private final CasoRepository repositorio;
    private final MongoTemplate mongoTemplate;
    private final UsuariosClient usuariosClient;
    private final AuditoriaClient auditoria;

    public CasoService(CasoRepository repositorio, MongoTemplate mongoTemplate,
                       UsuariosClient usuariosClient, AuditoriaClient auditoria) {
        this.repositorio = repositorio;
        this.mongoTemplate = mongoTemplate;
        this.usuariosClient = usuariosClient;
        this.auditoria = auditoria;
    }

    /** INVESTIGADOR: registrar un caso nuevo (ABIERTO). */
    public CasoResponse crear(CasoRequest r) {
        String codigo = r.codigo().trim().toUpperCase();
        if (repositorio.existsByCodigo(codigo)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un caso con ese código");
        }
        Caso c = new Caso();
        c.setCodigo(codigo);
        c.setTitulo(r.titulo().trim());
        c.setDescripcion(r.descripcion().trim());
        c.setCreadoPor(UsuarioActual.correo());
        c.getHistorial().add(new CambioEstado(null, EstadoCaso.ABIERTO.name(),
                UsuarioActual.correo(), rolActual(), "Caso registrado", LocalDateTime.now()));
        Caso guardado = repositorio.save(c);
        auditoria.exito("CASO_CREADO", RECURSO, guardado.getId(), codigo);
        return aRespuesta(guardado);
    }

    public List<CasoResponse> buscar(String codigo, EstadoCaso estado, String investigador) {
        Query q = new Query().with(Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        if (codigo != null && !codigo.isBlank()) {
            q.addCriteria(Criteria.where("codigo").regex(Pattern.quote(codigo.trim()), "i"));
        }
        if (estado != null) {
            q.addCriteria(Criteria.where("estado").is(estado));
        }
        if (investigador != null && !investigador.isBlank()) {
            q.addCriteria(Criteria.where("investigadorAsignado").is(investigador.trim().toLowerCase()));
        }
        return mongoTemplate.find(q, Caso.class).stream().map(this::aRespuesta).toList();
    }

    public CasoResponse obtener(String id) {
        return aRespuesta(buscarCaso(id));
    }

    /** FISCAL: asignar (o reasignar) un investigador activo. */
    public CasoResponse asignar(String id, String investigadorCorreo) {
        Caso c = buscarCaso(id);
        exigirEstado(c, EstadoCaso.ABIERTO, EstadoCaso.ASIGNADO);
        String correo = investigadorCorreo.trim().toLowerCase();
        UsuariosClient.UsuarioRef investigador = usuariosClient.investigadores().stream()
                .filter(u -> correo.equalsIgnoreCase(u.correo()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "El usuario indicado no es un investigador activo"));
        c.setInvestigadorAsignado(correo);
        c.setInvestigadorNombre(investigador.nombreCompleto());
        c.setAsignadoPor(UsuarioActual.correo());
        c.cambiarEstado(EstadoCaso.ASIGNADO, UsuarioActual.correo(), rolActual(), "Asignado a " + correo);
        return guardar(c, "CASO_ASIGNADO", "Investigador " + correo);
    }

    /** INVESTIGADOR asignado: iniciar la investigación. */
    public CasoResponse iniciarInvestigacion(String id) {
        Caso c = buscarCaso(id);
        exigirEstado(c, EstadoCaso.ASIGNADO);
        exigirInvestigadorAsignado(c);
        c.cambiarEstado(EstadoCaso.EN_INVESTIGACION, UsuarioActual.correo(), rolActual(), "Investigación iniciada");
        return guardar(c, "CASO_INVESTIGACION_INICIADA", null);
    }

    /** INVESTIGADOR asignado: agregar una nota de investigación. */
    public CasoResponse agregarNota(String id, String texto) {
        Caso c = buscarCaso(id);
        exigirEstado(c, EstadoCaso.EN_INVESTIGACION);
        exigirInvestigadorAsignado(c);
        c.agregarNota(new Nota(texto.trim(), UsuarioActual.correo(), LocalDateTime.now()));
        return guardar(c, "CASO_NOTA_AGREGADA", recortar(texto));
    }

    /** FISCAL: suspender el caso con un motivo. */
    public CasoResponse suspender(String id, String motivo) {
        Caso c = buscarCaso(id);
        exigirEstado(c, EstadoCaso.ASIGNADO, EstadoCaso.EN_INVESTIGACION);
        c.setEstadoAntesDeSuspension(c.getEstado());
        c.setMotivoSuspension(motivo.trim());
        c.cambiarEstado(EstadoCaso.SUSPENDIDO, UsuarioActual.correo(), rolActual(), motivo.trim());
        return guardar(c, "CASO_SUSPENDIDO", recortar(motivo));
    }

    /** FISCAL: reactivar un caso suspendido (vuelve al estado que tenía). */
    public CasoResponse reactivar(String id, String observacion) {
        Caso c = buscarCaso(id);
        exigirEstado(c, EstadoCaso.SUSPENDIDO);
        EstadoCaso destino = c.getEstadoAntesDeSuspension() != null
                ? c.getEstadoAntesDeSuspension() : EstadoCaso.EN_INVESTIGACION;
        c.setMotivoSuspension(null);
        c.setEstadoAntesDeSuspension(null);
        c.cambiarEstado(destino, UsuarioActual.correo(), rolActual(), textoOpcional(observacion, "Caso reactivado"));
        return guardar(c, "CASO_REACTIVADO", observacion);
    }

    /** REVISOR_LEGAL: emitir el concepto legal del caso. */
    public CasoResponse emitirConcepto(String id, ConceptoLegal concepto, String fundamento) {
        Caso c = buscarCaso(id);
        exigirEstado(c, EstadoCaso.EN_INVESTIGACION, EstadoCaso.SUSPENDIDO);
        c.setConceptoLegal(concepto);
        c.setFundamentoLegal(fundamento.trim());
        c.setRevisorLegal(UsuarioActual.correo());
        c.setFechaConcepto(LocalDateTime.now());
        c.setFechaActualizacion(c.getFechaConcepto());
        return guardar(c, "CASO_CONCEPTO_LEGAL", concepto.name());
    }

    /** FISCAL: cerrar el caso; exige concepto legal FAVORABLE. */
    public CasoResponse cerrar(String id, String observacion) {
        Caso c = buscarCaso(id);
        exigirEstado(c, EstadoCaso.EN_INVESTIGACION);
        if (c.getConceptoLegal() != ConceptoLegal.FAVORABLE) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "El cierre requiere un concepto legal FAVORABLE emitido por el revisor legal");
        }
        c.cambiarEstado(EstadoCaso.CERRADO, UsuarioActual.correo(), rolActual(), textoOpcional(observacion, "Caso cerrado"));
        return guardar(c, "CASO_CERRADO", observacion);
    }

    private Caso buscarCaso(String id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Caso no encontrado"));
    }

    private CasoResponse guardar(Caso c, String accion, String detalle) {
        Caso guardado = repositorio.save(c);
        auditoria.exito(accion, RECURSO, guardado.getId(), guardado.getCodigo() + (detalle != null ? " · " + detalle : ""));
        return aRespuesta(guardado);
    }

    private static void exigirEstado(Caso c, EstadoCaso... permitidos) {
        if (c.getEstado() == null || !Arrays.asList(permitidos).contains(c.getEstado())) {
            throw new ApiException(HttpStatus.CONFLICT, "El caso está en estado " + c.getEstado()
                    + " y no permite esta acción (requiere " + Arrays.toString(permitidos) + ")");
        }
    }

    /** Casos antiguos sin investigador asignado aceptan a cualquier investigador. */
    private static void exigirInvestigadorAsignado(Caso c) {
        String asignado = c.getInvestigadorAsignado();
        if (asignado != null && !asignado.equalsIgnoreCase(UsuarioActual.correo())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el investigador asignado puede realizar esta acción");
        }
    }

    private static String rolActual() {
        Rol rol = UsuarioActual.rol();
        return rol != null ? rol.name() : null;
    }

    private static String textoOpcional(String texto, String porDefecto) {
        return texto != null && !texto.isBlank() ? texto.trim() : porDefecto;
    }

    private static String recortar(String texto) {
        String limpio = texto.trim();
        return limpio.length() > 120 ? limpio.substring(0, 120) + "…" : limpio;
    }

    private CasoResponse aRespuesta(Caso c) {
        return new CasoResponse(c.getId(), c.getCodigo(), c.getTitulo(), c.getDescripcion(), c.getEstado(),
                c.getCreadoPor(), c.getFechaCreacion(), c.getInvestigadorAsignado(), c.getInvestigadorNombre(),
                c.getAsignadoPor(), c.getMotivoSuspension(), c.getConceptoLegal(), c.getFundamentoLegal(),
                c.getRevisorLegal(), c.getFechaConcepto(), List.copyOf(c.getNotas()), List.copyOf(c.getHistorial()),
                c.getFechaActualizacion());
    }
}
