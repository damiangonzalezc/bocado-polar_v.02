package cl.bocadopolar.pedidos.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cl.bocadopolar.pedidos.model.EstadoPedido;
import cl.bocadopolar.pedidos.model.HistorialEstado;
import cl.bocadopolar.pedidos.model.ItemPedido;
import cl.bocadopolar.pedidos.model.MetodoPago;
import cl.bocadopolar.pedidos.model.Pedido;
import cl.bocadopolar.pedidos.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Pedidos de ejemplo. Los 7 primeros son "de hoy" (los usan ms-pagos, ms-rutas y ms-reparto en sus datos);
 * luego se genera un historial de ~40 días para que los KPI de ms-reportes tengan sentido.
 * Clientes (ms-clientes): 1 Cliente Demo, 2 María Soto, 3 Pedro Araya, 4 Camila Rojas, 5 Mar Azul, 6 El Faro.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bocado.semilla.habilitada", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {

    private static final Map<Long, String> NOMBRES = Map.of(1L, "Salmón Premium", 2L, "Filete de Reineta",
            3L, "Camarón Pelado", 4L, "Pulpo Cocido", 5L, "Merluza Austral", 6L, "Mix Mariscos");
    private static final Map<Long, Integer> PRECIOS = Map.of(1L, 12990, 2L, 8990, 3L, 10990, 4L, 14990,
            5L, 6990, 6L, 11990);
    private static final Map<String, Integer> ENVIO = Map.of("Concepción", 2990, "Chiguayante", 3490,
            "Tomé", 4490, "Penco", 4490);

    private record C(Long id, String nombre, String email, String direccion, String comuna) {
    }

    private static final List<C> CLIENTES = List.of(
            new C(1L, "Cliente Demo", "cliente@bocado.cl", "O'Higgins 1234", "Concepción"),
            new C(2L, "María Soto", "maria.soto@correo.cl", "Los Aromos 321", "Chiguayante"),
            new C(3L, "Pedro Araya", "pedro.araya@correo.cl", "Av. Collao 1550", "Concepción"),
            new C(4L, "Camila Rojas", "camila.rojas@correo.cl", "Sotomayor 45", "Tomé"),
            new C(5L, "Restaurante Mar Azul (B2B)", "ventas@marazul.cl", "Los Carrera 455", "Chiguayante"),
            new C(6L, "Cevichería El Faro", "compras@elfaro.cl", "Costanera 12", "Penco"));

    private final PedidoRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        LocalDate hoy = LocalDate.now();
        Instant inicioHoy = hoy.atTime(LocalTime.of(8, 0)).atZone(ZoneId.systemDefault()).toInstant();
        // ---- Pedidos de hoy (IDs 1 a 7) ----
        guardar(CLIENTES.get(0), null, "Tocar timbre 3B", hoy, "09:00 - 12:00", MetodoPago.WEBPAY,
                EstadoPedido.EN_PREPARACION, inicioHoy, item(1L, "3.5"));
        guardar(CLIENTES.get(4), null, "Entrada de proveedores, portón azul", hoy, "12:00 - 15:00",
                MetodoPago.CONTRA_ENTREGA, EstadoPedido.ENVIADO, inicioHoy.plusSeconds(600), item(4L, "12"));
        guardar(CLIENTES.get(0), new C(1L, "Cliente Demo", "cliente@bocado.cl", "Ejército 88", "Tomé"),
                "Dejar en conserjería", hoy, "15:00 - 18:00", MetodoPago.WEBPAY, EstadoPedido.EN_PREPARACION,
                inicioHoy.plusSeconds(1200), item(3L, "2"));
        guardar(CLIENTES.get(2), null, null, hoy.plusDays(1), "09:00 - 12:00", MetodoPago.TRANSFERENCIA,
                EstadoPedido.PAGADO, inicioHoy.plusSeconds(1800), item(2L, "4"), item(5L, "2"));
        guardar(CLIENTES.get(1), null, null, hoy.plusDays(1), "12:00 - 15:00", MetodoPago.WEBPAY,
                EstadoPedido.PENDIENTE_PAGO, Instant.now(), item(6L, "1.5"));
        guardar(CLIENTES.get(3), null, null, hoy, "09:00 - 12:00", MetodoPago.CONTRA_ENTREGA,
                EstadoPedido.ENTREGADO, inicioHoy.plusSeconds(2400), item(1L, "2"));
        guardar(CLIENTES.get(5), null, null, hoy, "12:00 - 15:00", MetodoPago.WEBPAY,
                EstadoPedido.EN_PREPARACION, inicioHoy.plusSeconds(3000), item(5L, "3"));

        // ---- Historial de 40 días (siempre igual gracias a la semilla del Random) ----
        Random rnd = new Random(2609);
        MetodoPago[] metodos = { MetodoPago.WEBPAY, MetodoPago.WEBPAY, MetodoPago.TRANSFERENCIA, MetodoPago.TARJETA,
                MetodoPago.CONTRA_ENTREGA };
        for (int dia = 40; dia >= 1; dia--) {
            int cuantos = rnd.nextDouble() < (dia < 14 ? 0.8 : 0.55) ? (rnd.nextDouble() < 0.3 ? 2 : 1) : 0;
            for (int k = 0; k < cuantos; k++) {
                C c = CLIENTES.get(rnd.nextInt(CLIENTES.size()));
                Instant creado = hoy.minusDays(dia).atTime(9 + rnd.nextInt(10), rnd.nextInt(60))
                        .atZone(ZoneId.systemDefault()).toInstant();
                long p1 = 1 + rnd.nextInt(6);
                long p2 = 1 + rnd.nextInt(6);
                ItemPedido a = item(p1, String.valueOf(1 + rnd.nextInt(8) * 0.5));
                double r = rnd.nextDouble();
                EstadoPedido estado = r < 0.06 ? EstadoPedido.CANCELADO : r < 0.1 ? EstadoPedido.REEMBOLSADO : EstadoPedido.ENTREGADO;
                MetodoPago metodo = metodos[rnd.nextInt(metodos.length)];
                LocalDate entrega = hoy.minusDays(dia - 1L);
                if (p2 != p1 && rnd.nextBoolean()) {
                    guardar(c, null, null, entrega, "09:00 - 12:00", metodo, estado, creado, a,
                            item(p2, String.valueOf(1 + rnd.nextInt(6) * 0.5)));
                } else {
                    guardar(c, null, null, entrega, "15:00 - 18:00", metodo, estado, creado, a);
                }
            }
        }
        log.info("ms-pedidos: {} pedidos de ejemplo creados", repository.count());
    }

    private ItemPedido item(Long productoId, String kg) {
        return new ItemPedido(null, productoId, NOMBRES.get(productoId), BigDecimal.valueOf(PRECIOS.get(productoId)),
                new BigDecimal(kg));
    }

    private void guardar(C c, C otraDireccion, String referencia, LocalDate entrega, String horario, MetodoPago metodo,
            EstadoPedido estado, Instant creado, ItemPedido... items) {
        C dir = otraDireccion == null ? c : otraDireccion;
        Pedido p = new Pedido();
        p.setCodigo("TMP-" + (System.nanoTime() % 100000000L));
        p.setClienteId(c.id());
        p.setNombreCliente(c.nombre());
        p.setEmail(c.email());
        p.setTelefono("+56 9 5555 1000");
        p.setDireccion(dir.direccion());
        p.setComuna(dir.comuna());
        p.setReferencia(referencia);
        p.setFechaEntrega(entrega);
        p.setHorario(horario);
        p.setMetodoPago(metodo);
        p.setEstado(estado);
        p.setCreadoEn(creado);
        p.setActualizadoEn(creado);
        p.getItems().addAll(List.of(items));
        p.setEnvio(BigDecimal.valueOf(ENVIO.get(dir.comuna())));
        p.recalcularTotal();
        historial(p, estado, creado);
        p = repository.save(p);
        p.setCodigo(String.format("BP-%06d", 100000 + p.getId()));
        repository.save(p);
    }

    private void historial(Pedido p, EstadoPedido estado, Instant t0) {
        boolean cod = p.getMetodoPago() == MetodoPago.CONTRA_ENTREGA;
        List<EstadoPedido> pasos;
        if (estado == EstadoPedido.CANCELADO) {
            pasos = List.of(EstadoPedido.PENDIENTE_PAGO, EstadoPedido.CANCELADO);
        } else if (estado == EstadoPedido.REEMBOLSADO) {
            pasos = List.of(EstadoPedido.PENDIENTE_PAGO, EstadoPedido.PAGADO, EstadoPedido.REEMBOLSADO);
        } else {
            List<EstadoPedido> flujo = cod
                    ? List.of(EstadoPedido.PENDIENTE_PAGO, EstadoPedido.EN_PREPARACION, EstadoPedido.ENVIADO, EstadoPedido.ENTREGADO)
                    : List.of(EstadoPedido.PENDIENTE_PAGO, EstadoPedido.PAGADO, EstadoPedido.EN_PREPARACION, EstadoPedido.ENVIADO, EstadoPedido.ENTREGADO);
            pasos = flujo.subList(0, flujo.indexOf(estado) + 1);
        }
        long[] minutos = { 0, 10, 180, 300, 360 };
        for (int i = 0; i < pasos.size(); i++) {
            p.getHistorial().add(new HistorialEstado(null, pasos.get(i), i == 0 ? "Pedido creado" : null, null,
                    t0.plus(minutos[Math.min(i, minutos.length - 1)], ChronoUnit.MINUTES), false));
        }
    }
}
