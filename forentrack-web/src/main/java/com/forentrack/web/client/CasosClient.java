package com.forentrack.web.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.forentrack.web.dto.CasoDto;
import com.forentrack.web.dto.CasoForm;

@Component
public class CasosClient extends ClienteApi {

    private static final ParameterizedTypeReference<List<CasoDto>> LISTA = new ParameterizedTypeReference<>() { };
    private static final String CASO = "/api/v1/casos/{id}";

    public CasosClient(@Value("${gateway.api.url}") String gatewayUrl) {
        super(gatewayUrl, "casos");
    }

    public CasoDto crear(String token, CasoForm f) {
        return enviar(HttpMethod.POST, token, "/api/v1/casos",
                mapa("codigo", f.getCodigo(), "titulo", f.getTitulo(), "descripcion", f.getDescripcion()), CasoDto.class);
    }

    public List<CasoDto> buscar(String token, String codigo, String estado, String investigador) {
        return consultarLista(token, "/api/v1/casos",
                mapa("codigo", codigo, "estado", estado, "investigador", investigador), LISTA);
    }

    public CasoDto obtener(String token, String id) {
        return consultar(token, CASO, null, CasoDto.class, id);
    }

    public CasoDto asignar(String token, String id, String investigadorCorreo) {
        return accion(token, id, "/asignacion", mapa("investigadorCorreo", investigadorCorreo));
    }

    public CasoDto iniciar(String token, String id) {
        return accion(token, id, "/inicio-investigacion", null);
    }

    public CasoDto agregarNota(String token, String id, String texto) {
        return accion(token, id, "/notas", mapa("texto", texto));
    }

    public CasoDto suspender(String token, String id, String motivo) {
        return accion(token, id, "/suspension", mapa("texto", motivo));
    }

    public CasoDto reactivar(String token, String id, String observacion) {
        return accion(token, id, "/reactivacion", mapa("observacion", observacion));
    }

    public CasoDto emitirConcepto(String token, String id, String concepto, String fundamento) {
        return accion(token, id, "/concepto-legal", mapa("concepto", concepto, "fundamento", fundamento));
    }

    public CasoDto cerrar(String token, String id, String observacion) {
        return accion(token, id, "/cierre", mapa("observacion", observacion));
    }

    private CasoDto accion(String token, String id, String sufijo, Object cuerpo) {
        return enviar(HttpMethod.POST, token, CASO + sufijo, cuerpo, CasoDto.class, id);
    }
}
