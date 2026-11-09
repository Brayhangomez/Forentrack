package com.forentrack.auditoria.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Evento de la bitácora. Solo se agrega información de revisión; el evento original no se modifica. */
@Document(collection = "eventos")
public class EventoAuditoria {

    @Id
    private String id;
    @Indexed
    private LocalDateTime fecha = LocalDateTime.now();
    private String servicio;
    @Indexed
    private String accion;
    private String resultado;
    @Indexed
    private String usuario;
    private String rol;
    private String recursoTipo;
    private String recursoId;
    private String detalle;

    private boolean revisado;
    private String revisadoPor;
    private String observacionRevision;
    private LocalDateTime fechaRevision;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getServicio() { return servicio; }
    public void setServicio(String servicio) { this.servicio = servicio; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getRecursoTipo() { return recursoTipo; }
    public void setRecursoTipo(String recursoTipo) { this.recursoTipo = recursoTipo; }
    public String getRecursoId() { return recursoId; }
    public void setRecursoId(String recursoId) { this.recursoId = recursoId; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
    public boolean isRevisado() { return revisado; }
    public void setRevisado(boolean revisado) { this.revisado = revisado; }
    public String getRevisadoPor() { return revisadoPor; }
    public void setRevisadoPor(String revisadoPor) { this.revisadoPor = revisadoPor; }
    public String getObservacionRevision() { return observacionRevision; }
    public void setObservacionRevision(String observacionRevision) { this.observacionRevision = observacionRevision; }
    public LocalDateTime getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(LocalDateTime fechaRevision) { this.fechaRevision = fechaRevision; }
}
