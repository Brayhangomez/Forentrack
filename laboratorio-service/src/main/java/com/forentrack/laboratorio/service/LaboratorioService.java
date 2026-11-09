package com.forentrack.laboratorio.service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.EnumSet;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.forentrack.laboratorio.client.AuditoriaClient;
import com.forentrack.laboratorio.client.EvidenciasClient;
import com.forentrack.laboratorio.client.EvidenciasClient.EvidenciaRef;
import com.forentrack.laboratorio.client.UsuariosClient;
import com.forentrack.laboratorio.dto.DictamenRequest;
import com.forentrack.laboratorio.dto.HashRequest;
import com.forentrack.laboratorio.dto.SolicitudRequest;
import com.forentrack.laboratorio.exception.ApiException;
import com.forentrack.laboratorio.model.CambioEstado;
import com.forentrack.laboratorio.model.Contador;
import com.forentrack.laboratorio.model.EstadoSolicitud;
import com.forentrack.laboratorio.model.Rol;
import com.forentrack.laboratorio.model.SolicitudAnalisis;
import com.forentrack.laboratorio.model.TipoAnalisis;
import com.forentrack.laboratorio.repository.SolicitudRepository;
import com.forentrack.laboratorio.security.UsuarioActual;

/** Flujo del laboratorio forense. Cada método público es una acción exclusiva de un rol. */
@Service
public class LaboratorioService {

    private static final String RECURSO = "SOLICITUD_ANALISIS";
    private static final EnumSet<EstadoSolicitud> ACTIVAS = EnumSet.of(EstadoSolicitud.SOLICITADA,
            EstadoSolicitud.ASIGNADA, EstadoSolicitud.EN_PROCESO, EstadoSolicitud.DICTAMINADA);

    private final SolicitudRepository repositorio;
    private final MongoTemplate mongoTemplate;
    private final EvidenciasClient evidencias;
    private final UsuariosClient usuarios;
    private final AuditoriaClient auditoria;

    public LaboratorioService(SolicitudRepository repositorio, MongoTemplate mongoTemplate,
                              EvidenciasClient evidencias, UsuariosClient usuarios, AuditoriaClient auditoria) {
        this.repositorio = repositorio;
        this.mongoTemplate = mongoTemplate;
        this.evidencias = evidencias;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
    }

    /** INVESTIGADOR: solicitar análisis de una evidencia que está EN_CUSTODIA. */
    public SolicitudAnalisis solicitar(SolicitudRequest r) {
        EvidenciaRef ev = evidencias.obtener(r.evidenciaId());
        if (!"EN_CUSTODIA".equals(ev.estadoActual())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "La evidencia debe estar EN_CUSTODIA para solicitar análisis (está " + ev.estadoActual() + ")");
        }
        if (repositorio.existsByEvidenciaIdAndEstadoIn(ev.id(), ACTIVAS)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una solicitud de análisis activa para esta evidencia");
        }
        SolicitudAnalisis s = new SolicitudAnalisis();
        s.setCodigo(siguienteCodigo());
        s.setEvidenciaId(ev.id());
        s.setEvidenciaCodigo(ev.codigo());
        s.setCasoCodigo(ev.casoCodigo());
        s.setTipoEvidencia(ev.tipo());
        s.setTipoAnalisis(TipoAnalisis.segunTipoEvidencia(ev.tipo()));
        s.setObjetivo(r.objetivo().trim());
        s.setPrioridad(r.prioridad());
        s.setSolicitadaPor(UsuarioActual.correo());
        s.getHistorial().add(new CambioEstado(null, EstadoSolicitud.SOLICITADA.name(),
                UsuarioActual.correo(), rolActual(), r.objetivo().trim(), LocalDateTime.now()));
        return guardar(s, "LAB_SOLICITUD_CREADA", ev.codigo() + " · " + s.getTipoAnalisis());
    }

    /** JEFE_LABORATORIO: asignar la solicitud a un perito (PERICIAL) o analista digital (DIGITAL). */
    public SolicitudAnalisis asignar(String id, String correo) {
        SolicitudAnalisis s = buscar(id);
        exigirEstado(s, EstadoSolicitud.SOLICITADA, EstadoSolicitud.ASIGNADA);
        String limpio = correo.trim().toLowerCase();
        String rolEsperado = s.getTipoAnalisis().getResponsable().name();
        UsuariosClient.UsuarioRef responsable = usuarios.personalLaboratorio().stream()
                .filter(u -> limpio.equalsIgnoreCase(u.correo()) && rolEsperado.equals(u.rol()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "El responsable debe ser un " + rolEsperado + " activo"));
        s.setAsignadoA(limpio);
        s.setAsignadoNombre(responsable.nombreCompleto());
        s.setAsignadoPor(UsuarioActual.correo());
        s.setFechaAsignacion(LocalDateTime.now());
        s.cambiarEstado(EstadoSolicitud.ASIGNADA, UsuarioActual.correo(), rolActual(), "Asignada a " + limpio);
        return guardar(s, "LAB_SOLICITUD_ASIGNADA", limpio);
    }

    /** PERITO (PERICIAL) o ANALISTA_DIGITAL (DIGITAL) asignado: iniciar el análisis (evidencia → EN_ANALISIS). */
    public SolicitudAnalisis iniciar(String id, TipoAnalisis tipo) {
        SolicitudAnalisis s = buscarDelResponsable(id, tipo);
        exigirEstado(s, EstadoSolicitud.ASIGNADA);
        evidencias.cambiarEstado(s.getEvidenciaId(), "EN_ANALISIS", "Análisis " + s.getCodigo() + " iniciado");
        s.setFechaInicio(LocalDateTime.now());
        s.cambiarEstado(EstadoSolicitud.EN_PROCESO, UsuarioActual.correo(), rolActual(), "Análisis iniciado");
        return guardar(s, tipo == TipoAnalisis.PERICIAL ? "LAB_PERITAJE_INICIADO" : "LAB_ANALISIS_DIGITAL_INICIADO", null);
    }

    /** ANALISTA_DIGITAL asignado: registrar la huella SHA-256 de la adquisición. */
    public SolicitudAnalisis registrarHash(String id, HashRequest r) {
        SolicitudAnalisis s = buscarDelResponsable(id, TipoAnalisis.DIGITAL);
        exigirEstado(s, EstadoSolicitud.EN_PROCESO);
        s.setHashSha256(r.hashSha256().toLowerCase());
        s.setHerramienta(r.herramienta().trim());
        return guardar(s, "LAB_HASH_REGISTRADO", s.getHashSha256());
    }

    /** PERITO (dictamen pericial) o ANALISTA_DIGITAL (informe técnico, exige hash) asignado. */
    public SolicitudAnalisis dictaminar(String id, TipoAnalisis tipo, DictamenRequest r) {
        SolicitudAnalisis s = buscarDelResponsable(id, tipo);
        exigirEstado(s, EstadoSolicitud.EN_PROCESO);
        if (tipo == TipoAnalisis.DIGITAL && s.getHashSha256() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Registre el hash SHA-256 antes de emitir el informe digital");
        }
        s.setHallazgos(r.hallazgos().trim());
        s.setConclusiones(r.conclusiones().trim());
        s.setFechaDictamen(LocalDateTime.now());
        s.cambiarEstado(EstadoSolicitud.DICTAMINADA, UsuarioActual.correo(), rolActual(), "Dictamen emitido");
        return guardar(s, tipo == TipoAnalisis.PERICIAL ? "LAB_DICTAMEN_PERICIAL" : "LAB_INFORME_DIGITAL", null);
    }

    /** JEFE_LABORATORIO: aprobar el dictamen (evidencia → ANALIZADA). */
    public SolicitudAnalisis aprobar(String id, String observacion) {
        SolicitudAnalisis s = buscar(id);
        exigirEstado(s, EstadoSolicitud.DICTAMINADA);
        evidencias.cambiarEstado(s.getEvidenciaId(), "ANALIZADA", "Dictamen " + s.getCodigo() + " aprobado");
        registrarRevision(s, observacion);
        s.cambiarEstado(EstadoSolicitud.APROBADA, UsuarioActual.correo(), rolActual(), "Dictamen aprobado");
        return guardar(s, "LAB_DICTAMEN_APROBADO", observacion);
    }

    /** JEFE_LABORATORIO: devolver el dictamen para corrección (DICTAMINADA → EN_PROCESO). */
    public SolicitudAnalisis rechazar(String id, String observacion) {
        if (observacion == null || observacion.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Indique la observación para el responsable");
        }
        SolicitudAnalisis s = buscar(id);
        exigirEstado(s, EstadoSolicitud.DICTAMINADA);
        registrarRevision(s, observacion);
        s.cambiarEstado(EstadoSolicitud.EN_PROCESO, UsuarioActual.correo(), rolActual(), observacion.trim());
        return guardar(s, "LAB_DICTAMEN_DEVUELTO", observacion);
    }

    public List<SolicitudAnalisis> listar(EstadoSolicitud estado, String asignadoA, String evidenciaId) {
        Query q = new Query().with(Sort.by(Sort.Direction.DESC, "fechaSolicitud"));
        if (estado != null) {
            q.addCriteria(Criteria.where("estado").is(estado));
        }
        if (asignadoA != null && !asignadoA.isBlank()) {
            q.addCriteria(Criteria.where("asignadoA").is(asignadoA.trim().toLowerCase()));
        }
        if (evidenciaId != null && !evidenciaId.isBlank()) {
            q.addCriteria(Criteria.where("evidenciaId").is(evidenciaId.trim()));
        }
        return mongoTemplate.find(q, SolicitudAnalisis.class);
    }

    public SolicitudAnalisis obtener(String id) {
        return buscar(id);
    }

    private void registrarRevision(SolicitudAnalisis s, String observacion) {
        s.setObservacionRevision(observacion != null && !observacion.isBlank() ? observacion.trim() : null);
        s.setRevisadoPor(UsuarioActual.correo());
        s.setFechaRevision(LocalDateTime.now());
    }

    private SolicitudAnalisis buscar(String id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud de análisis no encontrada"));
    }

    /** La solicitud debe ser del tipo del endpoint y estar asignada a quien la opera. */
    private SolicitudAnalisis buscarDelResponsable(String id, TipoAnalisis tipo) {
        SolicitudAnalisis s = buscar(id);
        if (s.getTipoAnalisis() != tipo) {
            throw new ApiException(HttpStatus.CONFLICT, "La solicitud es de análisis " + s.getTipoAnalisis());
        }
        if (s.getAsignadoA() == null || !s.getAsignadoA().equalsIgnoreCase(UsuarioActual.correo())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el responsable asignado puede operar esta solicitud");
        }
        return s;
    }

    private SolicitudAnalisis guardar(SolicitudAnalisis s, String accion, String detalle) {
        SolicitudAnalisis guardada = repositorio.save(s);
        auditoria.exito(accion, RECURSO, guardada.getId(),
                guardada.getCodigo() + (detalle != null && !detalle.isBlank() ? " · " + detalle : ""));
        return guardada;
    }

    private static void exigirEstado(SolicitudAnalisis s, EstadoSolicitud... permitidos) {
        if (!EnumSet.of(permitidos[0], permitidos).contains(s.getEstado())) {
            throw new ApiException(HttpStatus.CONFLICT, "La solicitud está " + s.getEstado()
                    + " y no permite esta acción");
        }
    }

    private static String rolActual() {
        Rol rol = UsuarioActual.rol();
        return rol != null ? rol.name() : null;
    }

    private String siguienteCodigo() {
        Contador c = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is("solicitud")),
                new Update().inc("valor", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                Contador.class);
        long valor = c != null ? c.getValor() : System.currentTimeMillis() % 100000;
        return String.format("SOL-%d-%05d", Year.now().getValue(), valor);
    }
}
