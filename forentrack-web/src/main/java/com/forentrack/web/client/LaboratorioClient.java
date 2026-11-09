package com.forentrack.web.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.forentrack.web.dto.SolicitudDto;

@Component
public class LaboratorioClient extends ClienteApi {

    private static final ParameterizedTypeReference<List<SolicitudDto>> LISTA = new ParameterizedTypeReference<>() { };
    private static final String BASE = "/api/v1/laboratorio/solicitudes";

    public LaboratorioClient(@Value("${gateway.api.url}") String gatewayUrl) {
        super(gatewayUrl, "laboratorio");
    }

    public SolicitudDto solicitar(String token, String evidenciaId, String objetivo, String prioridad) {
        return enviar(HttpMethod.POST, token, BASE, mapa("evidenciaId", evidenciaId, "objetivo", objetivo,
                "prioridad", prioridad), SolicitudDto.class);
    }

    public List<SolicitudDto> listar(String token, String estado, String asignadoA, String evidenciaId) {
        return consultarLista(token, BASE, mapa("estado", estado, "asignadoA", asignadoA, "evidenciaId", evidenciaId), LISTA);
    }

    public SolicitudDto asignar(String token, String id, String correo) {
        return accion(token, id, "/asignacion", mapa("responsableCorreo", correo));
    }

    public SolicitudDto aprobar(String token, String id, String observacion) {
        return accion(token, id, "/aprobacion", mapa("observacion", observacion));
    }

    public SolicitudDto rechazar(String token, String id, String observacion) {
        return accion(token, id, "/rechazo", mapa("observacion", observacion));
    }

    public SolicitudDto iniciarPeritaje(String token, String id) {
        return accion(token, id, "/peritaje/inicio", null);
    }

    public SolicitudDto dictamen(String token, String id, String hallazgos, String conclusiones) {
        return accion(token, id, "/peritaje/dictamen", mapa("hallazgos", hallazgos, "conclusiones", conclusiones));
    }

    public SolicitudDto iniciarDigital(String token, String id) {
        return accion(token, id, "/digital/inicio", null);
    }

    public SolicitudDto registrarHash(String token, String id, String hash, String herramienta) {
        return accion(token, id, "/digital/hash", mapa("hashSha256", hash, "herramienta", herramienta));
    }

    public SolicitudDto informeDigital(String token, String id, String hallazgos, String conclusiones) {
        return accion(token, id, "/digital/informe", mapa("hallazgos", hallazgos, "conclusiones", conclusiones));
    }

    private SolicitudDto accion(String token, String id, String sufijo, Object cuerpo) {
        return enviar(HttpMethod.POST, token, BASE + "/{id}" + sufijo, cuerpo, SolicitudDto.class, id);
    }
}
