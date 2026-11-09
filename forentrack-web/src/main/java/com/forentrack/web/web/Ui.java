package com.forentrack.web.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.forentrack.web.model.Rol;

/** Utilidades de presentación usadas desde las plantillas como ${@ui...}. Todas son seguras ante null. */
@Component("ui")
public class Ui {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Map<String, String> ETIQUETAS = Map.ofEntries(
            Map.entry("ABIERTO", "Abierto"), Map.entry("ASIGNADO", "Asignado"),
            Map.entry("EN_INVESTIGACION", "En investigación"), Map.entry("SUSPENDIDO", "Suspendido"),
            Map.entry("CERRADO", "Cerrado"), Map.entry("REGISTRADA", "Registrada"),
            Map.entry("EN_CUSTODIA", "En custodia"), Map.entry("EN_ANALISIS", "En análisis"),
            Map.entry("ANALIZADA", "Analizada"), Map.entry("DEVUELTA", "Devuelta"),
            Map.entry("DESCARTADA", "Descartada"), Map.entry("SOLICITADA", "Solicitada"),
            Map.entry("ASIGNADA", "Asignada"), Map.entry("EN_PROCESO", "En proceso"),
            Map.entry("DICTAMINADA", "Dictaminada"), Map.entry("APROBADA", "Aprobada"),
            Map.entry("EXITO", "Éxito"), Map.entry("FALLIDO", "Fallido"), Map.entry("DENEGADO", "Denegado"),
            Map.entry("FAVORABLE", "Favorable"), Map.entry("DESFAVORABLE", "Desfavorable"),
            Map.entry("VALIDA", "Válida"), Map.entry("OBSERVADA", "Observada"),
            Map.entry("FISICA", "Física"), Map.entry("DIGITAL", "Digital"), Map.entry("DOCUMENTAL", "Documental"),
            Map.entry("PERICIAL", "Pericial"), Map.entry("ALTA", "Alta"), Map.entry("MEDIA", "Media"),
            Map.entry("BAJA", "Baja"), Map.entry("ENTREGA", "Entrega"), Map.entry("RECEPCION", "Recepción"),
            Map.entry("TRASLADO", "Traslado"), Map.entry("DEVOLUCION", "Devolución"));

    private static final Map<String, String> TONOS = Map.ofEntries(
            Map.entry("ABIERTO", "azul"), Map.entry("ASIGNADO", "indigo"), Map.entry("EN_INVESTIGACION", "ambar"),
            Map.entry("SUSPENDIDO", "naranja"), Map.entry("CERRADO", "gris"),
            Map.entry("REGISTRADA", "azul"), Map.entry("EN_CUSTODIA", "indigo"), Map.entry("EN_ANALISIS", "ambar"),
            Map.entry("ANALIZADA", "verde"), Map.entry("DEVUELTA", "gris"), Map.entry("DESCARTADA", "rojo"),
            Map.entry("SOLICITADA", "azul"), Map.entry("ASIGNADA", "indigo"), Map.entry("EN_PROCESO", "ambar"),
            Map.entry("DICTAMINADA", "violeta"), Map.entry("APROBADA", "verde"),
            Map.entry("EXITO", "verde"), Map.entry("FALLIDO", "ambar"), Map.entry("DENEGADO", "rojo"),
            Map.entry("FAVORABLE", "verde"), Map.entry("DESFAVORABLE", "rojo"),
            Map.entry("VALIDA", "verde"), Map.entry("OBSERVADA", "ambar"),
            Map.entry("ALTA", "rojo"), Map.entry("MEDIA", "ambar"), Map.entry("BAJA", "gris"),
            Map.entry("ENTREGA", "azul"), Map.entry("RECEPCION", "indigo"), Map.entry("TRASLADO", "violeta"),
            Map.entry("DEVOLUCION", "verde"));

    private static final Map<String, String> ICONOS = Map.of(
            "FISICA", "bi-box", "DIGITAL", "bi-device-hdd", "DOCUMENTAL", "bi-file-earmark-text",
            "ENTREGA", "bi-box-arrow-right", "RECEPCION", "bi-box-arrow-in-down", "TRASLADO", "bi-truck",
            "DEVOLUCION", "bi-box-arrow-up");

    /** Texto legible de un valor de enum (estados, tipos, roles...). */
    public String etiqueta(Object valor) {
        if (valor == null) {
            return "—";
        }
        String clave = valor.toString();
        Rol rol = Rol.desde(clave);
        if (rol != null) {
            return rol.getEtiqueta();
        }
        String etiqueta = ETIQUETAS.get(clave);
        if (etiqueta != null) {
            return etiqueta;
        }
        String texto = clave.replace('_', ' ').toLowerCase();
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    /** Clase CSS del color del badge. */
    public String tono(Object valor) {
        return "t-" + (valor == null ? "gris" : TONOS.getOrDefault(valor.toString(), "gris"));
    }

    public String icono(Object valor) {
        return valor == null ? "bi-dot" : ICONOS.getOrDefault(valor.toString(), "bi-dot");
    }

    /** Fecha ISO (como la envían los servicios) a dd/MM/yyyy HH:mm. */
    public String fecha(String iso) {
        if (iso == null || iso.isBlank()) {
            return "—";
        }
        try {
            return LocalDateTime.parse(iso).format(FECHA_HORA);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(iso.length() >= 10 ? iso.substring(0, 10) : iso).format(FECHA);
            } catch (DateTimeParseException otra) {
                return iso;
            }
        }
    }

    public String recortar(String texto, int maximo) {
        if (texto == null) {
            return "";
        }
        return texto.length() > maximo ? texto.substring(0, maximo) + "…" : texto;
    }

    public String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "?";
        }
        String[] partes = nombre.trim().split("\\s+");
        String ini = partes[0].substring(0, 1) + (partes.length > 1 ? partes[1].substring(0, 1) : "");
        return ini.toUpperCase();
    }
}
