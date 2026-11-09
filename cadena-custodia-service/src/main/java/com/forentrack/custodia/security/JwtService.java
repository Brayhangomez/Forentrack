package com.forentrack.custodia.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Valida los JWT firmados por usuarios-service (secreto compartido JWT_SECRET) y deriva de ese mismo
 * secreto la clave interna que usan los servicios entre sí para operaciones solo de sistema.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final String claveInterna;

    public JwtService(@Value("${app.jwt.secret:}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("La variable JWT_SECRET es obligatoria y debe tener al menos 32 caracteres");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.claveInterna = derivarClaveInterna(secret);
    }

    public Claims leer(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }

    /** Clave para la cabecera X-Internal-Key (llamadas servicio a servicio). */
    public String claveInterna() {
        return claveInterna;
    }

    public boolean esClaveInternaValida(String valor) {
        return valor != null && MessageDigest.isEqual(
                valor.getBytes(StandardCharsets.UTF_8), claveInterna.getBytes(StandardCharsets.UTF_8));
    }

    private static String derivarClaveInterna(String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal("forentrack-interno".getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo derivar la clave interna", e);
        }
    }
}
