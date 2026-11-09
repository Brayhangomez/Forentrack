package com.forentrack.custodia.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.forentrack.custodia.client.AuditoriaClient;
import com.forentrack.custodia.client.EvidenciasClient;
import com.forentrack.custodia.client.EvidenciasClient.EvidenciaRef;
import com.forentrack.custodia.dto.DevolucionRequest;
import com.forentrack.custodia.dto.EntregaRequest;
import com.forentrack.custodia.dto.RecepcionRequest;
import com.forentrack.custodia.dto.TrasladoRequest;
import com.forentrack.custodia.dto.ValidacionRequest;
import com.forentrack.custodia.exception.ApiException;
import com.forentrack.custodia.model.Movimiento;
import com.forentrack.custodia.model.ResultadoValidacion;
import com.forentrack.custodia.model.Rol;
import com.forentrack.custodia.model.TipoMovimiento;
import com.forentrack.custodia.model.ValidacionCadena;
import com.forentrack.custodia.repository.MovimientoRepository;
import com.forentrack.custodia.repository.ValidacionRepository;
import com.forentrack.custodia.security.UsuarioActual;

/** Cadena de custodia: cada método es una acción exclusiva de un rol y deja un eslabón inmutable. */
@Service
public class CustodiaService {

    private static final String RECURSO = "EVIDENCIA";
    private static final int LIMITE = 200;

    private final MovimientoRepository movimientos;
    private final ValidacionRepository validaciones;
    private final EvidenciasClient evidencias;
    private final AuditoriaClient auditoria;

    public CustodiaService(MovimientoRepository movimientos, ValidacionRepository validaciones,
                           EvidenciasClient evidencias, AuditoriaClient auditoria) {
        this.movimientos = movimientos;
        this.validaciones = validaciones;
        this.evidencias = evidencias;
        this.auditoria = auditoria;
    }

    /** TECNICO_CAMPO: entregar la evidencia (con acta completa) a custodia. */
    public Movimiento entregar(EntregaRequest r) {
        EvidenciaRef ev = evidencias.obtener(r.evidenciaId());
        exigirEstado(ev, Set.of("REGISTRADA"));
        if (!ev.actaCompleta()) {
            throw new ApiException(HttpStatus.CONFLICT, "Complete el acta de recolección antes de entregar la evidencia");
        }
        if (movimientos.existsByEvidenciaIdAndTipo(ev.id(), TipoMovimiento.ENTREGA)) {
            throw new ApiException(HttpStatus.CONFLICT, "La evidencia ya fue entregada a custodia");
        }
        String destino = texto(r.destino(), "Bodega de evidencias");
        return guardar(ev, TipoMovimiento.ENTREGA, "Técnico de campo", destino, r.observacion(), "CUSTODIA_ENTREGA");
    }

    /** CUSTODIO: recibir en bodega una evidencia entregada (REGISTRADA → EN_CUSTODIA). */
    public Movimiento recibir(RecepcionRequest r) {
        EvidenciaRef ev = evidencias.obtener(r.evidenciaId());
        exigirEstado(ev, Set.of("REGISTRADA"));
        if (!movimientos.existsByEvidenciaIdAndTipo(ev.id(), TipoMovimiento.ENTREGA)) {
            throw new ApiException(HttpStatus.CONFLICT, "El técnico de campo aún no ha entregado esta evidencia");
        }
        evidencias.cambiarEstado(ev.id(), "EN_CUSTODIA", "Recibida en " + r.ubicacion().trim());
        return guardar(ev, TipoMovimiento.RECEPCION, "Entrega del técnico", r.ubicacion().trim(),
                r.observacion(), "CUSTODIA_RECEPCION");
    }

    /** CUSTODIO: trasladar o reubicar una evidencia bajo custodia. */
    public Movimiento trasladar(TrasladoRequest r) {
        EvidenciaRef ev = evidencias.obtener(r.evidenciaId());
        exigirEstado(ev, Set.of("EN_CUSTODIA", "EN_ANALISIS", "ANALIZADA"));
        String origen = ubicacionActual(ev.id());
        return guardar(ev, TipoMovimiento.TRASLADO, origen, r.destino().trim(), r.motivo(), "CUSTODIA_TRASLADO");
    }

    /** CUSTODIO: devolver una evidencia analizada; exige cadena validada por el revisor legal. */
    public Movimiento devolver(DevolucionRequest r) {
        EvidenciaRef ev = evidencias.obtener(r.evidenciaId());
        exigirEstado(ev, Set.of("ANALIZADA"));
        ResultadoValidacion ultima = validaciones.findFirstByEvidenciaIdOrderByFechaDesc(ev.id())
                .map(ValidacionCadena::getResultado).orElse(null);
        if (ultima != ResultadoValidacion.VALIDA) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "La cadena de custodia debe estar validada (VALIDA) por el revisor legal antes de devolver");
        }
        evidencias.cambiarEstado(ev.id(), "DEVUELTA", "Devuelta a " + r.destinatario().trim());
        return guardar(ev, TipoMovimiento.DEVOLUCION, ubicacionActual(ev.id()), r.destinatario().trim(),
                r.observacion(), "CUSTODIA_DEVOLUCION");
    }

    /** REVISOR_LEGAL: validar u observar la cadena de custodia de una evidencia. */
    public ValidacionCadena validar(String evidenciaId, ValidacionRequest r) {
        EvidenciaRef ev = evidencias.obtener(evidenciaId);
        if (!movimientos.existsByEvidenciaIdAndTipo(ev.id(), TipoMovimiento.RECEPCION)) {
            throw new ApiException(HttpStatus.CONFLICT, "La evidencia aún no tiene una cadena de custodia que validar");
        }
        ValidacionCadena v = new ValidacionCadena();
        v.setEvidenciaId(ev.id());
        v.setEvidenciaCodigo(ev.codigo());
        v.setResultado(r.resultado());
        v.setObservacion(r.observacion().trim());
        v.setRevisor(UsuarioActual.correo());
        ValidacionCadena guardada = validaciones.save(v);
        auditoria.exito("CUSTODIA_VALIDACION", RECURSO, ev.id(), ev.codigo() + ": " + r.resultado());
        return guardada;
    }

    public List<Movimiento> historial(String evidenciaId) {
        return movimientos.findByEvidenciaIdOrderByFechaAsc(evidenciaId);
    }

    public List<ValidacionCadena> validaciones(String evidenciaId) {
        return validaciones.findByEvidenciaIdOrderByFechaDesc(evidenciaId);
    }

    public List<Movimiento> recientes(TipoMovimiento tipo) {
        PageRequest pagina = PageRequest.of(0, LIMITE);
        return tipo != null ? movimientos.findByTipoOrderByFechaDesc(tipo, pagina)
                : movimientos.findAllByOrderByFechaDesc(pagina);
    }

    /** Entregas que el custodio todavía no ha recibido (bandeja de entrada del CUSTODIO). */
    public List<Movimiento> pendientesDeRecepcion() {
        return movimientos.findByTipoOrderByFechaDesc(TipoMovimiento.ENTREGA, PageRequest.of(0, LIMITE)).stream()
                .filter(m -> !movimientos.existsByEvidenciaIdAndTipo(m.getEvidenciaId(), TipoMovimiento.RECEPCION))
                .toList();
    }

    private String ubicacionActual(String evidenciaId) {
        return movimientos.findFirstByEvidenciaIdAndTipoInOrderByFechaDesc(evidenciaId,
                        EnumSet.of(TipoMovimiento.RECEPCION, TipoMovimiento.TRASLADO))
                .map(Movimiento::getDestino).orElse("Sin ubicación registrada");
    }

    private Movimiento guardar(EvidenciaRef ev, TipoMovimiento tipo, String origen, String destino,
                               String observacion, String accion) {
        Rol rol = UsuarioActual.rol();
        Movimiento m = new Movimiento();
        m.setEvidenciaId(ev.id());
        m.setEvidenciaCodigo(ev.codigo());
        m.setCasoCodigo(ev.casoCodigo());
        m.setTipo(tipo);
        m.setResponsable(UsuarioActual.correo());
        m.setRolResponsable(rol != null ? rol.name() : null);
        m.setOrigen(origen);
        m.setDestino(destino);
        m.setObservacion(observacion != null && !observacion.isBlank() ? observacion.trim() : null);
        Movimiento guardado = movimientos.save(m);
        auditoria.exito(accion, RECURSO, ev.id(), ev.codigo() + ": " + origen + " → " + destino);
        return guardado;
    }

    private static void exigirEstado(EvidenciaRef ev, Set<String> permitidos) {
        if (!permitidos.contains(ev.estadoActual())) {
            throw new ApiException(HttpStatus.CONFLICT, "La evidencia está " + ev.estadoActual()
                    + " y no permite esta acción (requiere " + String.join(" o ", permitidos) + ")");
        }
    }

    private static String texto(String valor, String porDefecto) {
        return valor != null && !valor.isBlank() ? valor.trim() : porDefecto;
    }
}
