package com.forentrack.web.client;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.forentrack.web.dto.EventoDto;
import com.forentrack.web.dto.ResumenDto;

@Component
public class AuditoriaClient extends ClienteApi {

    private static final ParameterizedTypeReference<List<EventoDto>> LISTA = new ParameterizedTypeReference<>() { };

    public AuditoriaClient(@Value("${gateway.api.url}") String gatewayUrl) {
        super(gatewayUrl, "auditoría");
    }

    public List<EventoDto> buscar(String token, Map<String, ?> filtros) {
        return consultarLista(token, "/api/v1/auditoria/eventos", filtros, LISTA);
    }

    public byte[] exportar(String token, Map<String, ?> filtros) {
        return consultar(token, "/api/v1/auditoria/eventos/exportar", filtros, byte[].class);
    }

    public EventoDto revisar(String token, String id, String observacion) {
        return enviar(HttpMethod.POST, token, "/api/v1/auditoria/eventos/{id}/revision",
                mapa("observacion", observacion), EventoDto.class, id);
    }

    public ResumenDto resumen(String token) {
        return consultar(token, "/api/v1/auditoria/resumen", null, ResumenDto.class);
    }
}
