package com.forentrack.custodia.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Eslabón inmutable de la cadena de custodia (nunca se edita ni se borra). */
@Document(collection = "movimientos")
public class Movimiento {

    @Id
    private String id;
    @Indexed
    private String evidenciaId;
    private String evidenciaCodigo;
    private String casoCodigo;
    private TipoMovimiento tipo;
    private String responsable;
    private String rolResponsable;
    private String origen;
    private String destino;
    private String observacion;
    private LocalDateTime fecha = LocalDateTime.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEvidenciaId() { return evidenciaId; }
    public void setEvidenciaId(String evidenciaId) { this.evidenciaId = evidenciaId; }
    public String getEvidenciaCodigo() { return evidenciaCodigo; }
    public void setEvidenciaCodigo(String evidenciaCodigo) { this.evidenciaCodigo = evidenciaCodigo; }
    public String getCasoCodigo() { return casoCodigo; }
    public void setCasoCodigo(String casoCodigo) { this.casoCodigo = casoCodigo; }
    public TipoMovimiento getTipo() { return tipo; }
    public void setTipo(TipoMovimiento tipo) { this.tipo = tipo; }
    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }
    public String getRolResponsable() { return rolResponsable; }
    public void setRolResponsable(String rolResponsable) { this.rolResponsable = rolResponsable; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
