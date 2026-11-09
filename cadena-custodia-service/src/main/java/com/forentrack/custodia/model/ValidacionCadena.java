package com.forentrack.custodia.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Revisión legal de la cadena de custodia de una evidencia (REVISOR_LEGAL). */
@Document(collection = "validaciones")
public class ValidacionCadena {

    @Id
    private String id;
    @Indexed
    private String evidenciaId;
    private String evidenciaCodigo;
    private ResultadoValidacion resultado;
    private String observacion;
    private String revisor;
    private LocalDateTime fecha = LocalDateTime.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEvidenciaId() { return evidenciaId; }
    public void setEvidenciaId(String evidenciaId) { this.evidenciaId = evidenciaId; }
    public String getEvidenciaCodigo() { return evidenciaCodigo; }
    public void setEvidenciaCodigo(String evidenciaCodigo) { this.evidenciaCodigo = evidenciaCodigo; }
    public ResultadoValidacion getResultado() { return resultado; }
    public void setResultado(ResultadoValidacion resultado) { this.resultado = resultado; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getRevisor() { return revisor; }
    public void setRevisor(String revisor) { this.revisor = revisor; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
