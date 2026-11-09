package com.forentrack.laboratorio.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Solicitud de análisis de laboratorio sobre una evidencia y su dictamen. */
@Document(collection = "solicitudes")
public class SolicitudAnalisis {

    @Id
    private String id;
    @Indexed(unique = true)
    private String codigo;
    @Indexed
    private String evidenciaId;
    private String evidenciaCodigo;
    private String casoCodigo;
    private String tipoEvidencia;
    private TipoAnalisis tipoAnalisis;
    private String objetivo;
    private Prioridad prioridad = Prioridad.MEDIA;
    private EstadoSolicitud estado = EstadoSolicitud.SOLICITADA;
    private String solicitadaPor;
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Indexed
    private String asignadoA;
    private String asignadoNombre;
    private String asignadoPor;
    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaInicio;

    private String hallazgos;
    private String conclusiones;
    private String hashSha256;
    private String herramienta;
    private LocalDateTime fechaDictamen;

    private String observacionRevision;
    private String revisadoPor;
    private LocalDateTime fechaRevision;

    private List<CambioEstado> historial = new ArrayList<>();

    public void cambiarEstado(EstadoSolicitud nuevo, String usuario, String rol, String observacion) {
        getHistorial().add(new CambioEstado(estado != null ? estado.name() : null, nuevo.name(),
                usuario, rol, observacion, LocalDateTime.now()));
        this.estado = nuevo;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getEvidenciaId() { return evidenciaId; }
    public void setEvidenciaId(String evidenciaId) { this.evidenciaId = evidenciaId; }
    public String getEvidenciaCodigo() { return evidenciaCodigo; }
    public void setEvidenciaCodigo(String evidenciaCodigo) { this.evidenciaCodigo = evidenciaCodigo; }
    public String getCasoCodigo() { return casoCodigo; }
    public void setCasoCodigo(String casoCodigo) { this.casoCodigo = casoCodigo; }
    public String getTipoEvidencia() { return tipoEvidencia; }
    public void setTipoEvidencia(String tipoEvidencia) { this.tipoEvidencia = tipoEvidencia; }
    public TipoAnalisis getTipoAnalisis() { return tipoAnalisis; }
    public void setTipoAnalisis(TipoAnalisis tipoAnalisis) { this.tipoAnalisis = tipoAnalisis; }
    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }
    public Prioridad getPrioridad() { return prioridad; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }
    public EstadoSolicitud getEstado() { return estado; }
    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }
    public String getSolicitadaPor() { return solicitadaPor; }
    public void setSolicitadaPor(String solicitadaPor) { this.solicitadaPor = solicitadaPor; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }
    public String getAsignadoA() { return asignadoA; }
    public void setAsignadoA(String asignadoA) { this.asignadoA = asignadoA; }
    public String getAsignadoNombre() { return asignadoNombre; }
    public void setAsignadoNombre(String asignadoNombre) { this.asignadoNombre = asignadoNombre; }
    public String getAsignadoPor() { return asignadoPor; }
    public void setAsignadoPor(String asignadoPor) { this.asignadoPor = asignadoPor; }
    public LocalDateTime getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(LocalDateTime fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
    public String getHallazgos() { return hallazgos; }
    public void setHallazgos(String hallazgos) { this.hallazgos = hallazgos; }
    public String getConclusiones() { return conclusiones; }
    public void setConclusiones(String conclusiones) { this.conclusiones = conclusiones; }
    public String getHashSha256() { return hashSha256; }
    public void setHashSha256(String hashSha256) { this.hashSha256 = hashSha256; }
    public String getHerramienta() { return herramienta; }
    public void setHerramienta(String herramienta) { this.herramienta = herramienta; }
    public LocalDateTime getFechaDictamen() { return fechaDictamen; }
    public void setFechaDictamen(LocalDateTime fechaDictamen) { this.fechaDictamen = fechaDictamen; }
    public String getObservacionRevision() { return observacionRevision; }
    public void setObservacionRevision(String observacionRevision) { this.observacionRevision = observacionRevision; }
    public String getRevisadoPor() { return revisadoPor; }
    public void setRevisadoPor(String revisadoPor) { this.revisadoPor = revisadoPor; }
    public LocalDateTime getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(LocalDateTime fechaRevision) { this.fechaRevision = fechaRevision; }
    public List<CambioEstado> getHistorial() {
        if (historial == null) {
            historial = new ArrayList<>();
        }
        return historial;
    }
    public void setHistorial(List<CambioEstado> historial) { this.historial = historial; }
}
