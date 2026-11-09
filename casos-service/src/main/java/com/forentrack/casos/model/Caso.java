package com.forentrack.casos.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Caso forense. Los campos nuevos son opcionales para que los documentos antiguos sigan siendo válidos. */
@Document(collection = "casos")
public class Caso {

    @Id
    private String id;
    @Indexed(unique = true)
    private String codigo;
    private String titulo;
    private String descripcion;
    private EstadoCaso estado = EstadoCaso.ABIERTO;
    private String creadoPor;
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Indexed
    private String investigadorAsignado;
    private String investigadorNombre;
    private String asignadoPor;
    private String motivoSuspension;
    private EstadoCaso estadoAntesDeSuspension;
    private ConceptoLegal conceptoLegal;
    private String fundamentoLegal;
    private String revisorLegal;
    private LocalDateTime fechaConcepto;
    private List<Nota> notas = new ArrayList<>();
    private List<CambioEstado> historial = new ArrayList<>();
    private LocalDateTime fechaActualizacion;

    /** Cambia el estado dejando constancia en el historial. */
    public void cambiarEstado(EstadoCaso nuevo, String usuario, String rol, String observacion) {
        LocalDateTime ahora = LocalDateTime.now();
        getHistorial().add(new CambioEstado(estado != null ? estado.name() : null, nuevo.name(),
                usuario, rol, observacion, ahora));
        this.estado = nuevo;
        this.fechaActualizacion = ahora;
    }

    public void agregarNota(Nota nota) {
        getNotas().add(nota);
        this.fechaActualizacion = nota.fecha();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public EstadoCaso getEstado() { return estado; }
    public void setEstado(EstadoCaso estado) { this.estado = estado; }
    public String getCreadoPor() { return creadoPor; }
    public void setCreadoPor(String creadoPor) { this.creadoPor = creadoPor; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getInvestigadorAsignado() { return investigadorAsignado; }
    public void setInvestigadorAsignado(String investigadorAsignado) { this.investigadorAsignado = investigadorAsignado; }
    public String getInvestigadorNombre() { return investigadorNombre; }
    public void setInvestigadorNombre(String investigadorNombre) { this.investigadorNombre = investigadorNombre; }
    public String getAsignadoPor() { return asignadoPor; }
    public void setAsignadoPor(String asignadoPor) { this.asignadoPor = asignadoPor; }
    public String getMotivoSuspension() { return motivoSuspension; }
    public void setMotivoSuspension(String motivoSuspension) { this.motivoSuspension = motivoSuspension; }
    public EstadoCaso getEstadoAntesDeSuspension() { return estadoAntesDeSuspension; }
    public void setEstadoAntesDeSuspension(EstadoCaso e) { this.estadoAntesDeSuspension = e; }
    public ConceptoLegal getConceptoLegal() { return conceptoLegal; }
    public void setConceptoLegal(ConceptoLegal conceptoLegal) { this.conceptoLegal = conceptoLegal; }
    public String getFundamentoLegal() { return fundamentoLegal; }
    public void setFundamentoLegal(String fundamentoLegal) { this.fundamentoLegal = fundamentoLegal; }
    public String getRevisorLegal() { return revisorLegal; }
    public void setRevisorLegal(String revisorLegal) { this.revisorLegal = revisorLegal; }
    public LocalDateTime getFechaConcepto() { return fechaConcepto; }
    public void setFechaConcepto(LocalDateTime fechaConcepto) { this.fechaConcepto = fechaConcepto; }
    public List<Nota> getNotas() {
        if (notas == null) {
            notas = new ArrayList<>(); // documentos creados antes de la versión 2
        }
        return notas;
    }
    public void setNotas(List<Nota> notas) { this.notas = notas; }
    public List<CambioEstado> getHistorial() {
        if (historial == null) {
            historial = new ArrayList<>();
        }
        return historial;
    }
    public void setHistorial(List<CambioEstado> historial) { this.historial = historial; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
