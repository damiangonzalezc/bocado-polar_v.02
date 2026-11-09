package cl.bocadopolar.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tabla de rutas: qué prefijo de /api/v1 atiende cada microservicio.
 * Se quita el encabezado Origin antes de reenviar para que el CORS lo responda solo el gateway.
 */
@Configuration
public class RutasConfig {

    @Bean
    public RouteLocator rutas(RouteLocatorBuilder builder,
            @Value("${bocado.servicios.auth}") String auth,
            @Value("${bocado.servicios.clientes}") String clientes,
            @Value("${bocado.servicios.catalogo}") String catalogo,
            @Value("${bocado.servicios.inventario}") String inventario,
            @Value("${bocado.servicios.pedidos}") String pedidos,
            @Value("${bocado.servicios.pagos}") String pagos,
            @Value("${bocado.servicios.marketing}") String marketing,
            @Value("${bocado.servicios.rutas}") String rutas,
            @Value("${bocado.servicios.reparto}") String reparto,
            @Value("${bocado.servicios.frio}") String frio,
            @Value("${bocado.servicios.config}") String config,
            @Value("${bocado.servicios.reportes}") String reportes,
            @Value("${bocado.servicios.notificaciones}") String notificaciones) {
        return builder.routes()
                .route("ms-auth", r -> r.path("/api/v1/auth/**", "/api/v1/usuarios/**", "/api/v1/permisos/**",
                        "/api/v1/auditoria/**").filters(f -> f.removeRequestHeader("Origin")).uri(auth))
                .route("ms-clientes", r -> r.path("/api/v1/clientes/**")
                        .filters(f -> f.removeRequestHeader("Origin")).uri(clientes))
                .route("ms-catalogo", r -> r.path("/api/v1/productos/**", "/api/v1/categorias/**", "/api/v1/atributos/**")
                        .filters(f -> f.removeRequestHeader("Origin")).uri(catalogo))
                .route("ms-inventario", r -> r.path("/api/v1/lotes/**", "/api/v1/stock/**", "/api/v1/movimientos/**",
                        "/api/v1/reservas/**", "/api/v1/alertas-stock/**")
                        .filters(f -> f.removeRequestHeader("Origin")).uri(inventario))
                .route("ms-pedidos", r -> r.path("/api/v1/carritos/**", "/api/v1/pedidos/**")
                        .filters(f -> f.removeRequestHeader("Origin")).uri(pedidos))
                .route("ms-pagos", r -> r.path("/api/v1/pagos/**", "/api/v1/boletas/**", "/api/v1/reembolsos/**")
                        .filters(f -> f.removeRequestHeader("Origin")).uri(pagos))
                .route("ms-marketing", r -> r.path("/api/v1/cupones/**", "/api/v1/canales/**", "/api/v1/carritos-abandonados/**")
                        .filters(f -> f.removeRequestHeader("Origin")).uri(marketing))
                .route("ms-rutas", r -> r.path("/api/v1/rutas/**", "/api/v1/paradas/**", "/api/v1/vehiculos/**")
                        .filters(f -> f.removeRequestHeader("Origin")).uri(rutas))
                .route("ms-reparto", r -> r.path("/api/v1/repartidores/**", "/api/v1/turnos/**", "/api/v1/entregas/**",
                        "/api/v1/incidencias/**", "/api/v1/cierres/**")
                        .filters(f -> f.removeRequestHeader("Origin")).uri(reparto))
                .route("ms-frio", r -> r.path("/api/v1/temperaturas/**", "/api/v1/alertas-frio/**", "/api/v1/cuentas-regresivas/**")
                        .filters(f -> f.removeRequestHeader("Origin")).uri(frio))
                .route("ms-config", r -> r.path("/api/v1/configuracion/**", "/api/v1/zonas/**", "/api/v1/empresas-logistica/**",
                        "/api/v1/pasarelas/**", "/api/v1/reglas/**")
                        .filters(f -> f.removeRequestHeader("Origin")).uri(config))
                .route("ms-reportes", r -> r.path("/api/v1/kpis/**", "/api/v1/visitas/**")
                        .filters(f -> f.removeRequestHeader("Origin")).uri(reportes))
                .route("ms-notificaciones", r -> r.path("/api/v1/notificaciones/**", "/api/v1/plantillas/**")
                        .filters(f -> f.removeRequestHeader("Origin")).uri(notificaciones))
                .build();
    }
}
