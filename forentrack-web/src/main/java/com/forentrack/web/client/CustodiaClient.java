package com.forentrack.web.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.forentrack.web.dto.MovimientoDto;
import com.forentrack.web.dto.ValidacionDto;

@Component
public class CustodiaClient extends ClienteApi {

    private static final ParameterizedTypeReference<List<MovimientoDto>> MOVIMIENTOS = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<ValidacionDto>> VALIDACIONES = new ParameterizedTypeReference<>() { };

    public CustodiaClient(@Value("${gateway.api.url}") String gatewayUrl) {
        super(gatewayUrl, "cadena de custodia");
    }

    public MovimientoDto entregar(String token, String evidenciaId, String destino, String observacion) {
        return movimiento(token, "/entregas", mapa("evidenciaId", evidenciaId, "destino", destino, "observacion", observacion));
    }

    public MovimientoDto recibir(String token, String evidenciaId, String ubicacion, String observacion) {
        return movimiento(token, "/recepciones", mapa("evidenciaId", evidenciaId, "ubicacion", ubicacion,
                "observacion", observacion));
    }

    public MovimientoDto trasladar(String token, String evidenciaId, String destino, String motivo) {
        return movimiento(token, "/traslados", mapa("evidenciaId", evidenciaId, "destino", destino, "motivo", motivo));
    }

    public MovimientoDto devolver(String token, String evidenciaId, String destinatario, String observacion) {
        return movimiento(token, "/devoluciones", mapa("evidenciaId", evidenciaId, "destinatario", destinatario,
                "observacion", observacion));
    }

    public ValidacionDto validar(String token, String evidenciaId, String resultado, String observacion) {
        return enviar(HttpMethod.POST, token, "/api/v1/custodia/evidencias/{id}/validaciones",
                mapa("resultado", resultado, "observacion", observacion), ValidacionDto.class, evidenciaId);
    }

    public List<MovimientoDto> historial(String token, String evidenciaId) {
        return consultarLista(token, "/api/v1/custodia/evidencias/{id}/movimientos", null, MOVIMIENTOS, evidenciaId);
    }

    public List<ValidacionDto> validaciones(String token, String evidenciaId) {
        return consultarLista(token, "/api/v1/custodia/evidencias/{id}/validaciones", null, VALIDACIONES, evidenciaId);
    }

    public List<MovimientoDto> recientes(String token, String tipo) {
        return consultarLista(token, "/api/v1/custodia/movimientos", mapa("tipo", tipo), MOVIMIENTOS);
    }

    public List<MovimientoDto> pendientes(String token) {
        return consultarLista(token, "/api/v1/custodia/pendientes", null, MOVIMIENTOS);
    }

    private MovimientoDto movimiento(String token, String ruta, Object cuerpo) {
        return enviar(HttpMethod.POST, token, "/api/v1/custodia" + ruta, cuerpo, MovimientoDto.class);
    }
}
