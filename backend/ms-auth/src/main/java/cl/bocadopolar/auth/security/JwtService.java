package cl.bocadopolar.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cl.bocadopolar.auth.exception.NoAutorizadoException;
import cl.bocadopolar.auth.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Emite y lee los JWT. El API Gateway valida la firma con el mismo secreto. */
@Component
public class JwtService {

    private final SecretKey clave;
    private final long horas;

    public JwtService(@Value("${bocado.jwt.secreto}") String secreto, @Value("${bocado.jwt.horas:8}") long horas) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.horas = horas;
    }

    public TokenGenerado generar(Usuario u) {
        String jti = UUID.randomUUID().toString();
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(horas, ChronoUnit.HOURS);
        String token = Jwts.builder()
                .id(jti)
                .subject(u.getEmail())
                .claim("uid", u.getId())
                .claim("rol", u.getRol().name())
                .claim("nombre", u.nombreCompleto())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira))
                .signWith(clave)
                .compact();
        return new TokenGenerado(token, jti, ahora, expira);
    }

    public Claims leer(String token) {
        try {
            return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new NoAutorizadoException("Token inválido o expirado. Inicia sesión nuevamente.");
        }
    }

    public static Long usuarioId(Claims claims) {
        Number n = claims.get("uid", Number.class);
        return n == null ? null : n.longValue();
    }

    /** Extrae el token de "Authorization: Bearer xxx". */
    public static String extraer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() < 8) {
            throw new NoAutorizadoException("Falta el encabezado Authorization: Bearer <token>");
        }
        return authorization.substring(7).trim();
    }

    public record TokenGenerado(String token, String jti, Instant emitido, Instant expira) {
    }
}
