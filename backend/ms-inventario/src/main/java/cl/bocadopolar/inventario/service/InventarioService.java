package cl.bocadopolar.inventario.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bocadopolar.inventario.client.CatalogoClient;
import cl.bocadopolar.inventario.client.CatalogoClient.ProductoDto;
import cl.bocadopolar.inventario.client.ConfigClient;
import cl.bocadopolar.inventario.client.NotificacionesClient;
import cl.bocadopolar.inventario.dto.InventarioDtos.AjusteMasivoItem;
import cl.bocadopolar.inventario.dto.InventarioDtos.AjusteRequest;
import cl.bocadopolar.inventario.dto.InventarioDtos.ItemReserva;
import cl.bocadopolar.inventario.dto.InventarioDtos.LoteRequest;
import cl.bocadopolar.inventario.dto.InventarioDtos.LoteUpdateRequest;
import cl.bocadopolar.inventario.dto.InventarioDtos.ReservaRequest;
import cl.bocadopolar.inventario.dto.InventarioDtos.StockProducto;
import cl.bocadopolar.inventario.exception.RecursoNoEncontradoException;
import cl.bocadopolar.inventario.exception.ReglaNegocioException;
import cl.bocadopolar.inventario.exception.SolicitudInvalidaException;
import cl.bocadopolar.inventario.model.AlertaStock;
import cl.bocadopolar.inventario.model.EstadoReserva;
import cl.bocadopolar.inventario.model.Lote;
import cl.bocadopolar.inventario.model.MovimientoStock;
import cl.bocadopolar.inventario.model.ReservaStock;
import cl.bocadopolar.inventario.model.TipoMovimiento;
import cl.bocadopolar.inventario.repository.AlertaStockRepository;
import cl.bocadopolar.inventario.repository.LoteRepository;
import cl.bocadopolar.inventario.repository.MovimientoStockRepository;
import cl.bocadopolar.inventario.repository.ReservaStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventarioService {

    private final LoteRepository loteRepository;
    private final MovimientoStockRepository movimientoRepository;
    private final ReservaStockRepository reservaRepository;
    private final AlertaStockRepository alertaRepository;
    private final CatalogoClient catalogoClient;
    private final ConfigClient configClient;
    private final NotificacionesClient notificaciones;

    /* =============== Lotes =============== */

    public List<Lote> listarLotes(Long productoId, Boolean bloqueado, boolean incluirAgotados) {
        List<Lote> base = productoId == null ? loteRepository.findAll()
                : loteRepository.findByProductoIdOrderByVencimientoAsc(productoId);
        return base.stream()
                .filter(l -> bloqueado == null || bloqueado.equals(l.getBloqueado()))
                .filter(l -> incluirAgotados || l.getStockKg().signum() > 0)
                .sorted(Comparator.comparing(Lote::getVencimiento))
                .toList();
    }

    public Lote obtenerLote(Long id) {
        return loteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el lote " + id));
    }

    /** CU-04.01 Registrar lote (entrada a bodega). */
    @Transactional
    public Lote registrarLote(LoteRequest req) {
        String codigo = req.codigoLote().trim().toUpperCase();
        if (loteRepository.existsByCodigoLoteIgnoreCase(codigo)) {
            throw new ReglaNegocioException("Ya existe el lote " + codigo);
        }
        if (req.vencimiento().isBefore(LocalDate.now())) {
            throw new SolicitudInvalidaException("El lote ya está vencido");
        }
        ProductoDto producto = catalogoClient.producto(req.productoId());
        Lote l = new Lote();
        l.setProductoId(req.productoId());
        l.setProductoNombre(producto == null ? null : producto.nombre());
        l.setCodigoLote(codigo);
        l.setStockKg(req.stockKg());
        l.setReservadoKg(BigDecimal.ZERO);
        l.setVencimiento(req.vencimiento());
        l.setTempAlmacenaje(req.tempAlmacenaje());
        l.setBloqueado(false);
        l.setIngreso(Instant.now());
        l = loteRepository.save(l);
        registrarMovimiento(l, TipoMovimiento.ENTRADA, req.stockKg(), "Ingreso de lote " + codigo, req.usuarioId());
        verificarCritico(l.getProductoId());
        return l;
    }

    @Transactional
    public Lote actualizarLote(Long id, LoteUpdateRequest req) {
        Lote l = obtenerLote(id);
        String codigo = req.codigoLote().trim().toUpperCase();
        if (!codigo.equalsIgnoreCase(l.getCodigoLote()) && loteRepository.existsByCodigoLoteIgnoreCase(codigo)) {
            throw new ReglaNegocioException("Ya existe el lote " + codigo);
        }
        l.setCodigoLote(codigo);
        l.setVencimiento(req.vencimiento());
        l.setTempAlmacenaje(req.tempAlmacenaje());
        return loteRepository.save(l);
    }

    @Transactional
    public void eliminarLote(Long id) {
        Lote l = obtenerLote(id);
        if (l.getReservadoKg().signum() > 0) {
            throw new ReglaNegocioException("El lote tiene " + l.getReservadoKg() + " kg reservados para pedidos");
        }
        loteRepository.delete(l);
        verificarCritico(l.getProductoId());
    }

    /** CU-04.03 Lotes utilizables de un producto en orden FEFO. */
    public List<Lote> fefo(Long productoId) {
        return loteRepository.findByProductoIdOrderByVencimientoAsc(productoId).stream()
                .filter(Lote::utilizable)
                .toList();
    }

    /* =============== Ajustes de stock =============== */

    /** CU-04.02 Ajuste individual. */
    @Transactional
    public MovimientoStock ajustar(Long loteId, AjusteRequest req) {
        Lote l = obtenerLote(loteId);
        MovimientoStock m = aplicarAjuste(l, req.tipo(), req.kg(), req.motivo(), req.usuarioId());
        verificarCritico(l.getProductoId());
        return m;
    }

    /** CU-04.02 Ajuste masivo: todo o nada. */
    @Transactional
    public List<MovimientoStock> ajustarMasivo(List<AjusteMasivoItem> items, Long usuarioId) {
        List<MovimientoStock> movimientos = new ArrayList<>();
        for (AjusteMasivoItem item : items) {
            Lote l = obtenerLote(item.loteId());
            movimientos.add(aplicarAjuste(l, item.tipo(), item.kg(), item.motivo(), usuarioId));
        }
        items.stream().map(i -> obtenerLote(i.loteId()).getProductoId()).distinct().forEach(this::verificarCritico);
        return movimientos;
    }

    private MovimientoStock aplicarAjuste(Lote l, TipoMovimiento tipo, BigDecimal kg, String motivo, Long usuarioId) {
        if (kg.signum() == 0) {
            throw new SolicitudInvalidaException("Los kg del ajuste no pueden ser 0");
        }
        BigDecimal delta = switch (tipo) {
            case ENTRADA -> kg.abs();
            case SALIDA, MERMA -> kg.abs().negate();
            case AJUSTE -> kg;
        };
        BigDecimal nuevo = l.getStockKg().add(delta);
        if (nuevo.compareTo(l.getReservadoKg()) < 0) {
            throw new ReglaNegocioException("El lote " + l.getCodigoLote() + " quedaría con " + nuevo
                    + " kg y tiene " + l.getReservadoKg() + " kg reservados");
        }
        l.setStockKg(nuevo);
        loteRepository.save(l);
        return registrarMovimiento(l, tipo, delta, motivo, usuarioId);
    }

    /* =============== Bloqueo (cadena de frío) =============== */

    @Transactional
    public Lote bloquear(Long id, String motivo) {
        Lote l = obtenerLote(id);
        l.setBloqueado(true);
        l.setMotivoBloqueo(motivo);
        loteRepository.save(l);
        verificarCritico(l.getProductoId());
        return l;
    }

    @Transactional
    public Lote desbloquear(Long id) {
        Lote l = obtenerLote(id);
        l.setBloqueado(false);
        l.setMotivoBloqueo(null);
        loteRepository.save(l);
        verificarCritico(l.getProductoId());
        return l;
    }

    /* =============== Stock por producto =============== */

    public StockProducto stock(Long productoId) {
        List<Lote> lotes = loteRepository.findByProductoIdOrderByVencimientoAsc(productoId);
        return resumir(productoId, lotes);
    }

    public List<StockProducto> stockGeneral() {
        Map<Long, List<Lote>> porProducto = loteRepository.findAll().stream()
                .collect(Collectors.groupingBy(Lote::getProductoId, LinkedHashMap::new, Collectors.toList()));
        return porProducto.entrySet().stream()
                .map(e -> resumir(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(StockProducto::disponibleKg))
                .toList();
    }

    private StockProducto resumir(Long productoId, List<Lote> lotes) {
        BigDecimal stock = BigDecimal.ZERO;
        BigDecimal reservado = BigDecimal.ZERO;
        BigDecimal disponible = BigDecimal.ZERO;
        LocalDate proximo = null;
        String nombre = null;
        for (Lote l : lotes) {
            stock = stock.add(l.getStockKg());
            reservado = reservado.add(l.getReservadoKg());
            if (l.utilizable()) {
                disponible = disponible.add(l.getDisponibleKg());
                if (proximo == null || l.getVencimiento().isBefore(proximo)) {
                    proximo = l.getVencimiento();
                }
            }
            nombre = nombre == null ? l.getProductoNombre() : nombre;
        }
        boolean critico = disponible.compareTo(umbralCritico()) < 0;
        return new StockProducto(productoId, nombre, stock, reservado, disponible, lotes.size(), critico, proximo);
    }

    public List<MovimientoStock> movimientos(Long loteId, Long productoId) {
        if (loteId != null) {
            return movimientoRepository.findByLoteIdOrderByFechaDesc(loteId);
        }
        if (productoId != null) {
            return movimientoRepository.findByProductoIdOrderByFechaDesc(productoId);
        }
        return movimientoRepository.findTop200ByOrderByFechaDesc();
    }

    /* =============== Reservas para pedidos =============== */

    /** CU-04.05 Reserva FEFO de todos los ítems del pedido: si falta stock de uno, no se reserva nada. */
    @Transactional
    public List<ReservaStock> reservar(ReservaRequest req) {
        List<ReservaStock> previas = reservaRepository.findByPedidoId(req.pedidoId()).stream()
                .filter(r -> r.getEstado() == EstadoReserva.RESERVADA || r.getEstado() == EstadoReserva.CONFIRMADA)
                .toList();
        if (!previas.isEmpty()) {
            return previas;
        }
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(configClient.regla("RESERVA_MIN", BigDecimal.valueOf(30)).longValue(), ChronoUnit.MINUTES);
        List<ReservaStock> creadas = new ArrayList<>();
        for (ItemReserva item : req.items()) {
            BigDecimal pendiente = item.kg();
            List<Lote> lotes = fefo(item.productoId());
            BigDecimal disponible = lotes.stream().map(Lote::getDisponibleKg).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (disponible.compareTo(pendiente) < 0) {
                String nombre = lotes.isEmpty() ? "producto " + item.productoId() : lotes.get(0).getProductoNombre();
                throw new ReglaNegocioException("Stock insuficiente de " + nombre + ": hay " + disponible
                        + " kg disponibles y se piden " + pendiente + " kg");
            }
            for (Lote l : lotes) {
                if (pendiente.signum() <= 0) {
                    break;
                }
                BigDecimal toma = l.getDisponibleKg().min(pendiente);
                l.setReservadoKg(l.getReservadoKg().add(toma));
                loteRepository.save(l);
                creadas.add(reservaRepository.save(new ReservaStock(null, req.pedidoId(), l.getId(), l.getProductoId(),
                        toma, EstadoReserva.RESERVADA, ahora, expira)));
                pendiente = pendiente.subtract(toma);
            }
        }
        req.items().stream().map(ItemReserva::productoId).distinct().forEach(this::verificarCritico);
        return creadas;
    }

    public List<ReservaStock> reservasDe(Long pedidoId) {
        return reservaRepository.findByPedidoId(pedidoId);
    }

    /** Pago aprobado (o contra entrega): los kg reservados salen definitivamente del lote. */
    @Transactional
    public List<ReservaStock> confirmar(Long pedidoId) {
        List<ReservaStock> reservas = reservaRepository.findByPedidoId(pedidoId);
        List<ReservaStock> activas = reservas.stream().filter(r -> r.getEstado() == EstadoReserva.RESERVADA).toList();
        if (activas.isEmpty()) {
            if (reservas.stream().anyMatch(r -> r.getEstado() == EstadoReserva.CONFIRMADA)) {
                return reservas;
            }
            throw new ReglaNegocioException("El pedido " + pedidoId + " no tiene stock reservado (pudo haber expirado)");
        }
        for (ReservaStock r : activas) {
            Lote l = obtenerLote(r.getLoteId());
            l.setReservadoKg(l.getReservadoKg().subtract(r.getKg()).max(BigDecimal.ZERO));
            l.setStockKg(l.getStockKg().subtract(r.getKg()));
            loteRepository.save(l);
            registrarMovimiento(l, TipoMovimiento.SALIDA, r.getKg().negate(), "Pedido " + pedidoId, null);
            r.setEstado(EstadoReserva.CONFIRMADA);
            reservaRepository.save(r);
        }
        return reservas;
    }

    /** CU-04.06 Cancelación: libera lo reservado y devuelve a bodega lo que ya se había descontado. */
    @Transactional
    public List<ReservaStock> liberar(Long pedidoId, EstadoReserva estadoFinal) {
        List<ReservaStock> reservas = reservaRepository.findByPedidoId(pedidoId);
        for (ReservaStock r : reservas) {
            if (r.getEstado() == EstadoReserva.RESERVADA) {
                Lote l = obtenerLote(r.getLoteId());
                l.setReservadoKg(l.getReservadoKg().subtract(r.getKg()).max(BigDecimal.ZERO));
                loteRepository.save(l);
                r.setEstado(estadoFinal);
                reservaRepository.save(r);
            } else if (r.getEstado() == EstadoReserva.CONFIRMADA) {
                Lote l = obtenerLote(r.getLoteId());
                l.setStockKg(l.getStockKg().add(r.getKg()));
                loteRepository.save(l);
                registrarMovimiento(l, TipoMovimiento.ENTRADA, r.getKg(), "Devolución por cancelación del pedido " + pedidoId, null);
                r.setEstado(EstadoReserva.LIBERADA);
                reservaRepository.save(r);
            }
        }
        reservas.stream().map(ReservaStock::getProductoId).distinct().forEach(this::verificarCritico);
        return reservas;
    }

    /** CU-04.10 Expirar reservas sin pago (lo ejecuta TareasProgramadas cada minuto). */
    @Transactional
    public int expirarReservas() {
        List<ReservaStock> vencidas = reservaRepository.findByEstadoAndExpiraEnBefore(EstadoReserva.RESERVADA, Instant.now());
        vencidas.stream().map(ReservaStock::getPedidoId).distinct()
                .forEach(pedidoId -> liberar(pedidoId, EstadoReserva.EXPIRADA));
        return vencidas.size();
    }

    /* =============== Stock crítico =============== */

    public List<AlertaStock> alertas(Boolean atendida) {
        return atendida == null ? alertaRepository.findAllByOrderByFechaDesc()
                : alertaRepository.findByAtendidaOrderByFechaDesc(atendida);
    }

    @Transactional
    public AlertaStock resolverAlerta(Long id) {
        AlertaStock a = alertaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la alerta " + id));
        a.setAtendida(true);
        return alertaRepository.save(a);
    }

    /** CU-04.04 Revisa todos los productos y devuelve las alertas abiertas. */
    @Transactional
    public List<AlertaStock> detectarStockCritico() {
        loteRepository.findAll().stream().map(Lote::getProductoId).distinct().forEach(this::verificarCritico);
        return alertaRepository.findByAtendidaOrderByFechaDesc(false);
    }

    void verificarCritico(Long productoId) {
        StockProducto s = stock(productoId);
        List<AlertaStock> abiertas = alertaRepository.findByProductoIdAndAtendidaFalse(productoId);
        if (s.critico() && abiertas.isEmpty()) {
            alertaRepository.save(new AlertaStock(null, productoId, s.productoNombre(), s.disponibleKg(), false, Instant.now()));
            Map<String, Object> vars = new LinkedHashMap<>();
            vars.put("producto", Objects.toString(s.productoNombre(), "Producto " + productoId));
            vars.put("kg", s.disponibleKg());
            vars.put("umbral", umbralCritico());
            notificaciones.enviar("ALERTA_STOCK", "PANEL_ADMIN", "admin", vars);
        } else if (!s.critico() && !abiertas.isEmpty()) {
            abiertas.forEach(a -> a.setAtendida(true));
            alertaRepository.saveAll(abiertas);
        }
    }

    private BigDecimal umbralCritico() {
        return configClient.regla("STOCK_CRITICO", BigDecimal.valueOf(35));
    }

    private MovimientoStock registrarMovimiento(Lote l, TipoMovimiento tipo, BigDecimal kg, String motivo, Long usuarioId) {
        return movimientoRepository.save(new MovimientoStock(null, l.getId(), l.getProductoId(), tipo, motivo, kg,
                Instant.now(), usuarioId));
    }
}
