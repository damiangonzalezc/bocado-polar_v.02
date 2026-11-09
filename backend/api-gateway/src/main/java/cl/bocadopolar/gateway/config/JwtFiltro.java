package cl.bocadopolar.gateway.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

/**
 * Valida el JWT (Authorization: Bearer ...) antes de reenviar al microservicio.
 * - Rutas públicas: login, registro, catálogo, carrito, tarifas, visitas, validar cupón y webhook de pago.
 * - Rutas solo ADMIN: usuarios, permisos, auditoría, asignar pedidos a repartidor y cambios de configuración.
 * - Al microservicio le llegan los encabezados X-Usuario-Id, X-Usuario-Email y X-Usuario-Rol.
 */
@Component
public class JwtFiltro implements GlobalFilter, Ordered {

    private record Regla(HttpMethod metodo, String patron) {
    }

    private static final List<Regla> PUBLICAS = List.of(
            new Regla(HttpMethod.POST, "/api/v1/auth/login"),
            new Regla(HttpMethod.POST, "/api/v1/auth/registro"),
            new Regla(HttpMethod.GET, "/api/v1/productos/**"),
            new Regla(HttpMethod.GET, "/api/v1/categorias/**"),
            new Regla(HttpMethod.GET, "/api/v1/atributos/**"),
            new Regla(HttpMethod.GET, "/api/v1/stock/**"),
            new Regla(HttpMethod.GET, "/api/v1/lotes/fefo/**"),
            new Regla(null, "/api/v1/carritos/**"),
            new Regla(HttpMethod.GET, "/api/v1/zonas/**"),
            new Regla(HttpMethod.GET, "/api/v1/configuracion/**"),
            new Regla(HttpMethod.GET, "/api/v1/pasarelas/**"),
            new Regla(HttpMethod.POST, "/api/v1/visitas"),
            new Regla(HttpMethod.POST, "/api/v1/cupones/validar"),
            new Regla(HttpMethod.POST, "/api/v1/pagos/webhook"),
            new Regla(HttpMethod.GET, "/api/v1/boletas/*/html"));

    private static final List<Regla> SOLO_ADMIN = List.of(
            new Regla(null, "/api/v1/usuarios/**"),
            new Regla(null, "/api/v1/permisos/**"),
            new Regla(null, "/api/v1/auditoria/**"),
            new Regla(HttpMethod.POST, "/api/v1/rutas/asignar-pedido"),
            new Regla(HttpMethod.PATCH, "/api/v1/pedidos/*/repartidor"),
            new Regla(HttpMethod.PUT, "/api/v1/configuracion/**"),
            new Regla(HttpMethod.POST, "/api/v1/zonas/**"),
            new Regla(HttpMethod.PUT, "/api/v1/zonas/**"),
            new Regla(HttpMethod.PATCH, "/api/v1/zonas/**"),
            new Regla(HttpMethod.DELETE, "/api/v1/zonas/**"),
            new Regla(HttpMethod.PATCH, "/api/v1/pasarelas/**"),
            new Regla(null, "/api/v1/empresas-logistica/**"),
            new Regla(HttpMethod.POST, "/api/v1/reglas/**"),
            new Regla(HttpMethod.PUT, "/api/v1/reglas/**"),
            new Regla(HttpMethod.DELETE, "/api/v1/reglas/**"));

    private static final Set<String> CABECERAS_INTERNAS = Set.of("X-Usuario-Id", "X-Usuario-Email", "X-Usuario-Rol");

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final SecretKey clave;
    private final boolean habilitada;

    public JwtFiltro(@Value("${bocado.jwt.secreto}") String secreto,
            @Value("${bocado.seguridad.habilitada:true}") boolean habilitada) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.habilitada = habilitada;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        // nadie puede inyectar estos encabezados desde afuera
        ServerHttpRequest.Builder limpio = req.mutate().headers(h -> CABECERAS_INTERNAS.forEach(h::remove));
        String ruta = req.getPath().value();
        HttpMethod metodo = req.getMethod();

        if (!habilitada || HttpMethod.OPTIONS.equals(metodo) || coincide(PUBLICAS, metodo, ruta)) {
            return chain.filter(exchange.mutate().request(limpio.build()).build());
        }
        String auth = req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            return error(exchange, HttpStatus.UNAUTHORIZED, "Debes iniciar sesión (falta el token Bearer)");
        }
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(auth.substring(7).trim()).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return error(exchange, HttpStatus.UNAUTHORIZED, "Token inválido o expirado. Inicia sesión nuevamente.");
        }
        String rol = claims.get("rol", String.class);
        if (coincide(SOLO_ADMIN, metodo, ruta) && !"ADMIN".equals(rol)) {
            return error(exchange, HttpStatus.FORBIDDEN, "Esta acción es solo para el administrador");
        }
        Number uid = claims.get("uid", Number.class);
        limpio.header("X-Usuario-Id", uid == null ? "" : String.valueOf(uid.longValue()))
                .header("X-Usuario-Email", claims.getSubject() == null ? "" : claims.getSubject())
                .header("X-Usuario-Rol", rol == null ? "" : rol);
        return chain.filter(exchange.mutate().request(limpio.build()).build());
    }

    private boolean coincide(List<Regla> reglas, HttpMethod metodo, String ruta) {
        return reglas.stream().anyMatch(r -> (r.metodo() == null || r.metodo().equals(metodo))
                && matcher.match(r.patron(), ruta));
    }

    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status, String mensaje) {
        var resp = exchange.getResponse();
        resp.setStatusCode(status);
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"status\":" + status.value() + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"mensaje\":\"" + mensaje + "\",\"ruta\":\"" + exchange.getRequest().getPath().value() + "\"}";
        DataBuffer buffer = resp.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return resp.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
