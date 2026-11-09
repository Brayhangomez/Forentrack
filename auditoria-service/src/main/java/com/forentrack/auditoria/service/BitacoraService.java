package com.forentrack.auditoria.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.forentrack.auditoria.dto.EventoRequest;
import com.forentrack.auditoria.dto.FiltroEventos;
import com.forentrack.auditoria.dto.ResumenResponse;
import com.forentrack.auditoria.exception.ApiException;
import com.forentrack.auditoria.model.EventoAuditoria;
import com.forentrack.auditoria.model.Rol;
import com.forentrack.auditoria.repository.EventoRepository;
import com.forentrack.auditoria.security.JwtService;
import com.forentrack.auditoria.security.UsuarioActual;

/**
 * Bitácora central. Recibe los eventos de los otros servicios y registra directamente los propios
 * (este servicio no se llama a sí mismo por HTTP).
 */
@Service
public class BitacoraService {

    private static final int LIMITE_MAXIMO = 2000;
    private static final Set<String> RESULTADOS_ALERTA = Set.of("DENEGADO", "FALLIDO");

    private final EventoRepository repositorio;
    private final MongoTemplate mongoTemplate;
    private final JwtService jwtService;
    private final String servicio;

    public BitacoraService(EventoRepository repositorio, MongoTemplate mongoTemplate, JwtService jwtService,
                           @Value("${spring.application.name}") String servicio) {
        this.repositorio = repositorio;
        this.mongoTemplate = mongoTemplate;
        this.jwtService = jwtService;
        this.servicio = servicio;
    }

    /** Evento enviado por otro servicio. Si viene un JWT válido, el usuario y el rol salen del token. */
    public EventoAuditoria recibir(EventoRequest r, String claveInterna) {
        if (!jwtService.esClaveInternaValida(claveInterna)) {
            throw new AccessDeniedException("Solo los servicios de ForenTrack pueden registrar eventos");
        }
        EventoAuditoria e = new EventoAuditoria();
        e.setServicio(r.servicio());
        e.setAccion(r.accion());
        e.setResultado(r.resultado());
        String correo = UsuarioActual.correo();
        Rol rol = UsuarioActual.rol();
        e.setUsuario(correo != null ? correo : r.usuario());
        e.setRol(rol != null ? rol.name() : r.rol());
        e.setRecursoTipo(r.recursoTipo());
        e.setRecursoId(r.recursoId());
        e.setDetalle(r.detalle());
        return repositorio.save(e);
    }

    /** Acción exitosa realizada en este servicio (misma firma que AuditoriaClient). */
    public void exito(String accion, String recursoTipo, String recursoId, String detalle) {
        guardarLocal(accion, "EXITO", recursoTipo, recursoId, detalle);
    }

    /** Intento de acceso no permitido en este servicio (misma firma que AuditoriaClient). */
    public void denegado(String metodo, String ruta) {
        guardarLocal("ACCESO_DENEGADO", "DENEGADO", "RUTA", metodo + " " + ruta, "Intento sin permiso");
    }

    /** AUDITOR: consultar la bitácora con filtros. */
    public List<EventoAuditoria> buscar(FiltroEventos f, Integer limite) {
        int tope = limite == null || limite < 1 ? 500 : Math.min(limite, LIMITE_MAXIMO);
        return mongoTemplate.find(consulta(f).limit(tope), EventoAuditoria.class);
    }

    /** AUDITOR: exportar la bitácora filtrada en CSV (separador ; y BOM para Excel). */
    public byte[] exportarCsv(FiltroEventos f) {
        List<EventoAuditoria> eventos = mongoTemplate.find(consulta(f).limit(LIMITE_MAXIMO * 5), EventoAuditoria.class);
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("fecha;servicio;accion;resultado;usuario;rol;recurso_tipo;recurso_id;detalle;revisado;revisado_por;observacion_revision\n");
        for (EventoAuditoria e : eventos) {
            sb.append(String.join(";", csv(e.getFecha()), csv(e.getServicio()), csv(e.getAccion()),
                    csv(e.getResultado()), csv(e.getUsuario()), csv(e.getRol()), csv(e.getRecursoTipo()),
                    csv(e.getRecursoId()), csv(e.getDetalle()), csv(e.isRevisado() ? "SI" : "NO"),
                    csv(e.getRevisadoPor()), csv(e.getObservacionRevision()))).append('\n');
        }
        guardarLocal("AUDITORIA_EXPORTADA", "EXITO", "BITACORA", null, eventos.size() + " eventos");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** AUDITOR: registrar la revisión (hallazgo) de un evento. */
    public EventoAuditoria revisar(String id, String observacion) {
        EventoAuditoria e = repositorio.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
        if (e.isRevisado()) {
            throw new ApiException(HttpStatus.CONFLICT, "El evento ya fue revisado por " + e.getRevisadoPor());
        }
        e.setRevisado(true);
        e.setRevisadoPor(UsuarioActual.correo());
        e.setObservacionRevision(observacion.trim());
        e.setFechaRevision(LocalDateTime.now());
        EventoAuditoria guardado = repositorio.save(e);
        guardarLocal("AUDITORIA_EVENTO_REVISADO", "EXITO", "EVENTO", id, observacion.trim());
        return guardado;
    }

    /** AUDITOR: indicadores generales. */
    public ResumenResponse resumen() {
        return new ResumenResponse(repositorio.count(), repositorio.countByResultado("EXITO"),
                repositorio.countByResultado("FALLIDO"), repositorio.countByResultado("DENEGADO"),
                repositorio.countByRevisadoFalseAndResultadoIn(RESULTADOS_ALERTA),
                agrupar("servicio", 20), agrupar("accion", 10));
    }

    private Map<String, Long> agrupar(String campo, int maximo) {
        Aggregation agregacion = Aggregation.newAggregation(
                Aggregation.group(campo).count().as("total"),
                Aggregation.sort(Sort.Direction.DESC, "total"),
                Aggregation.limit(maximo));
        Map<String, Long> resultado = new LinkedHashMap<>();
        for (Document d : mongoTemplate.aggregate(agregacion, EventoAuditoria.class, Document.class).getMappedResults()) {
            Object clave = d.get("_id");
            Object total = d.get("total");
            resultado.put(clave != null ? clave.toString() : "(sin dato)",
                    total instanceof Number n ? n.longValue() : 0L);
        }
        return resultado;
    }

    private Query consulta(FiltroEventos f) {
        Query q = new Query().with(Sort.by(Sort.Direction.DESC, "fecha"));
        if (f == null) {
            return q;
        }
        if (texto(f.usuario())) {
            q.addCriteria(Criteria.where("usuario").regex(Pattern.quote(f.usuario().trim()), "i"));
        }
        if (texto(f.accion())) {
            q.addCriteria(Criteria.where("accion").is(f.accion().trim()));
        }
        if (texto(f.servicio())) {
            q.addCriteria(Criteria.where("servicio").is(f.servicio().trim()));
        }
        if (texto(f.resultado())) {
            q.addCriteria(Criteria.where("resultado").is(f.resultado().trim()));
        }
        if (f.revisado() != null) {
            q.addCriteria(Criteria.where("revisado").is(f.revisado()));
        }
        if (f.desde() != null || f.hasta() != null) {
            Criteria fecha = Criteria.where("fecha");
            if (f.desde() != null) {
                fecha = fecha.gte(f.desde().atStartOfDay());
            }
            if (f.hasta() != null) {
                fecha = fecha.lt(f.hasta().plusDays(1).atStartOfDay());
            }
            q.addCriteria(fecha);
        }
        return q;
    }

    private void guardarLocal(String accion, String resultado, String recursoTipo, String recursoId, String detalle) {
        EventoAuditoria e = new EventoAuditoria();
        e.setServicio(servicio);
        e.setAccion(accion);
        e.setResultado(resultado);
        e.setUsuario(UsuarioActual.correo());
        Rol rol = UsuarioActual.rol();
        e.setRol(rol != null ? rol.name() : null);
        e.setRecursoTipo(recursoTipo);
        e.setRecursoId(recursoId);
        e.setDetalle(detalle);
        repositorio.save(e);
    }

    private static boolean texto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String csv(Object valor) {
        if (valor == null) {
            return "";
        }
        return "\"" + valor.toString().replace("\"", "\"\"").replace('\n', ' ').replace('\r', ' ') + "\"";
    }
}
