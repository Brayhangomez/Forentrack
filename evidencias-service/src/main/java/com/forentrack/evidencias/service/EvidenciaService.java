package com.forentrack.evidencias.service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.forentrack.evidencias.client.AuditoriaClient;
import com.forentrack.evidencias.client.CasosClient;
import com.forentrack.evidencias.dto.CasoRef;
import com.forentrack.evidencias.dto.EvidenciaRequest;
import com.forentrack.evidencias.dto.EvidenciaResponse;
import com.forentrack.evidencias.dto.RecoleccionRequest;
import com.forentrack.evidencias.exception.ApiException;
import com.forentrack.evidencias.model.CambioEstado;
import com.forentrack.evidencias.model.Contador;
import com.forentrack.evidencias.model.EstadoEvidencia;
import com.forentrack.evidencias.model.Evidencia;
import com.forentrack.evidencias.model.Rol;
import com.forentrack.evidencias.model.TipoEvidencia;
import com.forentrack.evidencias.repository.EvidenciaRepository;
import com.forentrack.evidencias.security.JwtService;
import com.forentrack.evidencias.security.UsuarioActual;

@Service
public class EvidenciaService {

    private static final String RECURSO = "EVIDENCIA";
    private static final Set<String> CASO_ACTIVO = Set.of("ABIERTO", "ASIGNADO", "EN_INVESTIGACION");

    private final EvidenciaRepository repositorio;
    private final CasosClient casosClient;
    private final MongoTemplate mongoTemplate;
    private final JwtService jwtService;
    private final AuditoriaClient auditoria;

    public EvidenciaService(EvidenciaRepository repositorio, CasosClient casosClient, MongoTemplate mongoTemplate,
                            JwtService jwtService, AuditoriaClient auditoria) {
        this.repositorio = repositorio;
        this.casosClient = casosClient;
        this.mongoTemplate = mongoTemplate;
        this.jwtService = jwtService;
        this.auditoria = auditoria;
    }

    /** TECNICO_CAMPO: registrar evidencia en un caso activo. */
    public EvidenciaResponse registrar(EvidenciaRequest r) {
        CasoRef caso = casosClient.obtener(r.casoId().trim());
        if (!CASO_ACTIVO.contains(caso.estado())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "El caso está " + caso.estado() + " y no admite nuevas evidencias");
        }
        Evidencia e = new Evidencia();
        e.setCodigo(siguienteCodigo());
        e.setCasoId(caso.id());
        e.setCasoCodigo(caso.codigo());
        e.setTipo(r.tipo());
        e.setDescripcion(r.descripcion().trim());
        e.setRegistradoPor(UsuarioActual.correo());
        e.getHistorial().add(new CambioEstado(null, EstadoEvidencia.REGISTRADA.name(),
                UsuarioActual.correo(), rolActual(), "Evidencia registrada", e.getFechaRegistro()));
        Evidencia guardada = repositorio.save(e);
        auditoria.exito("EVIDENCIA_REGISTRADA", RECURSO, guardada.getId(),
                guardada.getCodigo() + " en caso " + caso.codigo());
        return aRespuesta(guardada);
    }

    public List<EvidenciaResponse> listar(String casoId, EstadoEvidencia estado, TipoEvidencia tipo) {
        Query q = new Query().with(Sort.by(Sort.Direction.DESC, "fechaRegistro"));
        if (casoId != null && !casoId.isBlank()) {
            q.addCriteria(Criteria.where("casoId").is(casoId.trim()));
        }
        if (estado != null) {
            q.addCriteria(Criteria.where("estadoActual").is(estado));
        }
        if (tipo != null) {
            q.addCriteria(Criteria.where("tipo").is(tipo));
        }
        return mongoTemplate.find(q, Evidencia.class).stream().map(this::aRespuesta).toList();
    }

    public EvidenciaResponse obtener(String id) {
        return aRespuesta(buscar(id));
    }

    /** TECNICO_CAMPO: completar el acta de recolección (solo mientras está REGISTRADA). */
    public EvidenciaResponse completarRecoleccion(String id, RecoleccionRequest r) {
        Evidencia e = buscar(id);
        if (e.getEstadoActual() != EstadoEvidencia.REGISTRADA) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "El acta solo se puede editar mientras la evidencia está REGISTRADA");
        }
        e.setLugarHallazgo(r.lugarHallazgo().trim());
        e.setFechaHallazgo(r.fechaHallazgo());
        e.setEmbalaje(r.embalaje().trim());
        e.setObservacionesRecoleccion(r.observaciones() != null ? r.observaciones().trim() : null);
        e.setFechaActualizacion(LocalDateTime.now());
        Evidencia guardada = repositorio.save(e);
        auditoria.exito("EVIDENCIA_ACTA_RECOLECCION", RECURSO, id, guardada.getCodigo());
        return aRespuesta(guardada);
    }

    /** REVISOR_LEGAL: autorizar el descarte de la evidencia. */
    public EvidenciaResponse descartar(String id, String motivo) {
        Evidencia e = buscar(id);
        e.setMotivoDescarte(motivo.trim());
        return transitar(e, EstadoEvidencia.DESCARTADA, motivo.trim(), "EVIDENCIA_DESCARTADA");
    }

    /**
     * Cambio de estado pedido por otro servicio (custodia o laboratorio). Exige la clave interna,
     * de modo que un usuario no puede saltarse el registro de custodia llamando directamente aquí.
     */
    public EvidenciaResponse cambiarEstadoInterno(String id, EstadoEvidencia destino, String observacion,
                                                  String claveInterna) {
        if (!jwtService.esClaveInternaValida(claveInterna)) {
            throw new AccessDeniedException("Operación reservada a servicios internos");
        }
        Evidencia e = buscar(id);
        if (destino == EstadoEvidencia.EN_ANALISIS) {
            exigirTipoSegunRol(e.getTipo());
        }
        return transitar(e, destino, observacion, "EVIDENCIA_" + destino.name());
    }

    private EvidenciaResponse transitar(Evidencia e, EstadoEvidencia destino, String observacion, String accion) {
        EstadoEvidencia actual = e.getEstadoActual() != null ? e.getEstadoActual() : EstadoEvidencia.REGISTRADA;
        Set<Rol> permitidos = actual.rolesPara(destino);
        if (permitidos.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "La evidencia está " + actual + " y no puede pasar a " + destino);
        }
        Rol rol = UsuarioActual.rol();
        if (rol == null || !permitidos.contains(rol)) {
            throw new AccessDeniedException("El rol no puede hacer la transición " + actual + " → " + destino);
        }
        e.cambiarEstado(destino, UsuarioActual.correo(), rol.name(), observacion);
        Evidencia guardada = repositorio.save(e);
        auditoria.exito(accion, RECURSO, guardada.getId(), guardada.getCodigo() + ": " + actual + " → " + destino);
        return aRespuesta(guardada);
    }

    /** El perito analiza evidencia física o documental; el analista digital, solo la digital. */
    private static void exigirTipoSegunRol(TipoEvidencia tipo) {
        Rol rol = UsuarioActual.rol();
        boolean digital = tipo == TipoEvidencia.DIGITAL;
        if ((rol == Rol.PERITO && digital) || (rol == Rol.ANALISTA_DIGITAL && !digital)) {
            throw new AccessDeniedException("El tipo de evidencia no corresponde al rol");
        }
    }

    private Evidencia buscar(String id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Evidencia no encontrada"));
    }

    private static String rolActual() {
        Rol rol = UsuarioActual.rol();
        return rol != null ? rol.name() : null;
    }

    // Código único EV-AAAA-NNNNN con contador atómico en MongoDB (HU-10)
    private String siguienteCodigo() {
        Contador c = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is("evidencia")),
                new Update().inc("valor", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                Contador.class);
        long valor = c != null ? c.getValor() : System.currentTimeMillis() % 100000;
        return String.format("EV-%d-%05d", Year.now().getValue(), valor);
    }

    private EvidenciaResponse aRespuesta(Evidencia e) {
        return new EvidenciaResponse(e.getId(), e.getCodigo(), e.getCasoId(), e.getCasoCodigo(),
                e.getTipo(), e.getDescripcion(), e.getEstadoActual(), e.getRegistradoPor(), e.getFechaRegistro(),
                e.getLugarHallazgo(), e.getFechaHallazgo(), e.getEmbalaje(), e.getObservacionesRecoleccion(),
                e.tieneActaCompleta(), e.getMotivoDescarte(), List.copyOf(e.getHistorial()), e.getFechaActualizacion());
    }
}
