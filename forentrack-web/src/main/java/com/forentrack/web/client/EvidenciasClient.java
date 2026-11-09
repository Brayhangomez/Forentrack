package com.forentrack.web.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.forentrack.web.dto.EvidenciaDto;
import com.forentrack.web.dto.EvidenciaForm;

@Component
public class EvidenciasClient extends ClienteApi {

    private static final ParameterizedTypeReference<List<EvidenciaDto>> LISTA = new ParameterizedTypeReference<>() { };

    public EvidenciasClient(@Value("${gateway.api.url}") String gatewayUrl) {
        super(gatewayUrl, "evidencias");
    }

    public EvidenciaDto crear(String token, EvidenciaForm f) {
        return enviar(HttpMethod.POST, token, "/api/v1/evidencias",
                mapa("casoId", f.getCasoId(), "tipo", f.getTipo(), "descripcion", f.getDescripcion()), EvidenciaDto.class);
    }

    public List<EvidenciaDto> listar(String token, String casoId, String estado, String tipo) {
        return consultarLista(token, "/api/v1/evidencias", mapa("casoId", casoId, "estado", estado, "tipo", tipo), LISTA);
    }

    public EvidenciaDto obtener(String token, String id) {
        return consultar(token, "/api/v1/evidencias/{id}", null, EvidenciaDto.class, id);
    }

    public EvidenciaDto completarActa(String token, String id, String lugar, String fecha, String embalaje,
                                      String observaciones) {
        return enviar(HttpMethod.PUT, token, "/api/v1/evidencias/{id}/recoleccion", mapa("lugarHallazgo", lugar,
                "fechaHallazgo", fecha, "embalaje", embalaje, "observaciones", observaciones), EvidenciaDto.class, id);
    }

    public EvidenciaDto descartar(String token, String id, String motivo) {
        return enviar(HttpMethod.POST, token, "/api/v1/evidencias/{id}/descarte", mapa("motivo", motivo),
                EvidenciaDto.class, id);
    }
}
