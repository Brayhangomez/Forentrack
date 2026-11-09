package com.forentrack.evidencias.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Evidencia forense. Los campos nuevos son opcionales para conservar los documentos existentes. */
@Document(collection = "evidencias")
public class Evidencia {

    @Id
    private String id;
    @Indexed(unique = true)
    private String codigo;
    @Indexed
    private String casoId;
    private String casoCodigo;
    private TipoEvidencia tipo;
    private String descripcion;
    private EstadoEvidencia estadoActual = EstadoEvidencia.REGISTRADA;
    private String registradoPor;
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    // Acta de recolección (TECNICO_CAMPO)
    private String lugarHallazgo;
    private LocalDateTime fechaHallazgo;
    private String embalaje;
    private String observacionesRecoleccion;

    private String motivoDescarte;
    private List<CambioEstado> historial = new ArrayList<>();
    private LocalDateTime fechaActualizacion;

    public void cambiarEstado(EstadoEvidencia nuevo, String usuario, String rol, String observacion) {
        LocalDateTime ahora = LocalDateTime.now();
        getHistorial().add(new CambioEstado(estadoActual != null ? estadoActual.name() : null, nuevo.name(),
                usuario, rol, observacion, ahora));
        this.estadoActual = nuevo;
        this.fechaActualizacion = ahora;
    }

    /** Calculado (no se guarda): el acta está completa si tiene lugar, fecha y embalaje. */
    public boolean tieneActaCompleta() {
        return lugarHallazgo != null && fechaHallazgo != null && embalaje != null;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getCasoId() { return casoId; }
    public void setCasoId(String casoId) { this.casoId = casoId; }
    public String getCasoCodigo() { return casoCodigo; }
    public void setCasoCodigo(String casoCodigo) { this.casoCodigo = casoCodigo; }
    public TipoEvidencia getTipo() { return tipo; }
    public void setTipo(TipoEvidencia tipo) { this.tipo = tipo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public EstadoEvidencia getEstadoActual() { return estadoActual; }
    public void setEstadoActual(EstadoEvidencia estadoActual) { this.estadoActual = estadoActual; }
    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public String getLugarHallazgo() { return lugarHallazgo; }
    public void setLugarHallazgo(String lugarHallazgo) { this.lugarHallazgo = lugarHallazgo; }
    public LocalDateTime getFechaHallazgo() { return fechaHallazgo; }
    public void setFechaHallazgo(LocalDateTime fechaHallazgo) { this.fechaHallazgo = fechaHallazgo; }
    public String getEmbalaje() { return embalaje; }
    public void setEmbalaje(String embalaje) { this.embalaje = embalaje; }
    public String getObservacionesRecoleccion() { return observacionesRecoleccion; }
    public void setObservacionesRecoleccion(String o) { this.observacionesRecoleccion = o; }
    public String getMotivoDescarte() { return motivoDescarte; }
    public void setMotivoDescarte(String motivoDescarte) { this.motivoDescarte = motivoDescarte; }
    public List<CambioEstado> getHistorial() {
        if (historial == null) {
            historial = new ArrayList<>(); // documentos creados antes de la versión 2
        }
        return historial;
    }
    public void setHistorial(List<CambioEstado> historial) { this.historial = historial; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
