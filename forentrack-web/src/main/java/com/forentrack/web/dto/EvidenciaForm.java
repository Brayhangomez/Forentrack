package com.forentrack.web.dto;

import jakarta.validation.constraints.*;

/** Formulario HTML con la misma validación que el microservicio. */
public class EvidenciaForm {
    @NotBlank(message = "Selecciona un caso")
    private String casoId;

    @NotBlank(message = "El tipo de evidencia es obligatorio")
    private String tipo;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
    private String descripcion;

    public String getCasoId() { return casoId; }
    public void setCasoId(String casoId) { this.casoId = casoId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
