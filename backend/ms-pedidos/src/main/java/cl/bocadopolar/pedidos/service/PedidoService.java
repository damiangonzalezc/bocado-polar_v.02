package cl.bocadopolar.pedidos.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bocadopolar.pedidos.client.Clientes.CatalogoClient;
import cl.bocadopolar.pedidos.client.Clientes.ClientesClient;
import cl.bocadopolar.pedidos.client.Clientes.ConfigClient;
import cl.bocadopolar.pedidos.client.Clientes.CuponDto;
import cl.bocadopolar.pedidos.client.Clientes.InventarioClient;
import cl.bocadopolar.pedidos.client.Clientes.MarketingClient;
import cl.bocadopolar.pedidos.client.Clientes.NotificacionesClient;
import cl.bocadopolar.pedidos.client.Clientes.ProductoDto;
import cl.bocadopolar.pedidos.dto.PedidoDtos;
import cl.bocadopolar.pedidos.dto.PedidoDtos.CheckoutRequest;
import cl.bocadopolar.pedidos.dto.PedidoDtos.CotizacionRequest;
import cl.bocadopolar.pedidos.dto.PedidoDtos.CotizacionResponse;
import cl.bocadopolar.pedidos.dto.PedidoDtos.EtiquetaResponse;
import cl.bocadopolar.pedidos.dto.PedidoDtos.PasoSeguimiento;
import cl.bocadopolar.pedidos.dto.PedidoDtos.SeguimientoResponse;
import cl.bocadopolar.pedidos.exception.RecursoNoEncontradoException;
import cl.bocadopolar.pedidos.exception.ReglaNegocioException;
import cl.bocadopolar.pedidos.exception.SolicitudInvalidaException;
import cl.bocadopolar.pedidos.model.Carrito;
import cl.bocadopolar.pedidos.model.EstadoPedido;
import cl.bocadopolar.pedidos.model.HistorialEstado;
import cl.bocadopolar.pedidos.model.ItemCarrito;
import cl.bocadopolar.pedidos.model.ItemPedido;
import cl.bocadopolar.pedidos.model.MetodoPago;
import cl.bocadopolar.pedidos.model.NotaInterna;
import cl.bocadopolar.pedidos.model.Pedido;
import cl.bocadopolar.pedidos.repository.CarritoRepository;
import cl.bocadopolar.pedidos.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService {

    /** Flujo normal que muestra la línea de tiempo del seguimiento. */
    public static final List<EstadoPedido> FLUJO = List.of(EstadoPedido.PENDIENTE_PAGO, EstadoPedido.PAGADO,
            EstadoPedido.EN_PREPARACION, EstadoPedido.ENVIADO, EstadoPedido.ENTREGADO);

    /** Transiciones permitidas hacia adelante (para volver atrás se usa "retroceder"). */
    public static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = new EnumMap<>(EstadoPedido.class);

    static {
        TRANSICIONES.put(EstadoPedido.PENDIENTE_PAGO, EnumSet.of(EstadoPedido.PAGADO, EstadoPedido.EN_PREPARACION, EstadoPedido.CANCELADO));
        TRANSICIONES.put(EstadoPedido.PAGADO, EnumSet.of(EstadoPedido.EN_PREPARACION, EstadoPedido.CANCELADO, EstadoPedido.REEMBOLSADO));
        TRANSICIONES.put(EstadoPedido.EN_PREPARACION, EnumSet.of(EstadoPedido.ENVIADO, EstadoPedido.CANCELADO, EstadoPedido.REEMBOLSADO));
        TRANSICIONES.put(EstadoPedido.ENVIADO, EnumSet.of(EstadoPedido.ENTREGADO, EstadoPedido.REEMBOLSADO));
        TRANSICIONES.put(EstadoPedido.ENTREGADO, EnumSet.of(EstadoPedido.REEMBOLSADO));
        TRANSICIONES.put(EstadoPedido.CANCELADO, EnumSet.of(EstadoPedido.REEMBOLSADO));
        TRANSICIONES.put(EstadoPedido.REEMBOLSADO, EnumSet.noneOf(EstadoPedido.class));
    }

    private static final Set<EstadoPedido> CON_STOCK_DESCONTADO = EnumSet.of(EstadoPedido.PAGADO,
            EstadoPedido.EN_PREPARACION, EstadoPedido.ENVIADO, EstadoPedido.ENTREGADO);

    private final PedidoRepository repository;
    private final CarritoRepository carritoRepository;
    private final CatalogoClient catalogo;
    private final ConfigClient config;
    private final MarketingClient marketing;
    private final InventarioClient inventario;
    private final ClientesClient clientes;
    private final NotificacionesClient notificaciones;

    /* =============== Checkout =============== */

    /** Calcula los totales sin crear el pedido (para mostrar el resumen antes de pagar). */
    public CotizacionResponse cotizar(CotizacionRequest req) {
        Carrito carrito = carritoRepository.findById(req.carritoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el carrito " + req.carritoId()));
        if (carrito.getItems().isEmpty()) {
            throw new ReglaNegocioException("El carrito está vacío");
        }
        BigDecimal subtotal = carrito.getSubtotal();
        BigDecimal envio = config.tarifa(req.comuna());
        BigDecimal descuento = BigDecimal.ZERO;
        String mensaje = null;
        if (req.codigoCupon() != null && !req.codigoCupon().isBlank()) {
            CuponDto cupon = marketing.validar(req.codigoCupon().trim().toUpperCase(), req.clienteId(), subtotal);
            mensaje = cupon.mensaje();
            if (Boolean.TRUE.equals(cupon.valido())) {
                descuento = cupon.descuento().min(subtotal);
            }
        }
        BigDecimal total = subtotal.add(envio).subtract(descuento).max(BigDecimal.ZERO);
        return new CotizacionResponse(subtotal, envio, descuento, total, carrito.getTotalKg(), mensaje);
    }

    /**
     * CU-05.02 Confirmar pedido: toma precios vigentes del catálogo, tarifa de la comuna, aplica cupón y
     * reserva el stock por FEFO en ms-inventario. Si algo falla no se crea nada.
     */
    @Transactional
    public Pedido checkout(CheckoutRequest req) {
        Carrito carrito = carritoRepository.findById(req.carritoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el carrito " + req.carritoId()));
        if (carrito.getItems().isEmpty()) {
            throw new ReglaNegocioException("El carrito está vacío");
        }
        if (carrito.getClienteId() != null && !carrito.getClienteId().equals(req.clienteId())) {
            throw new ReglaNegocioException("El carrito no pertenece a este cliente");
        }
        validarEntrega(req.fechaEntrega(), req.horario());

        // 1) Precios vigentes
        List<Long> ids = carrito.getItems().stream().map(ItemCarrito::getProductoId).toList();
        Map<Long, ProductoDto> productos = catalogo.productos(ids).stream()
                .collect(Collectors.toMap(ProductoDto::id, Function.identity()));
        Pedido p = new Pedido();
        for (ItemCarrito i : carrito.getItems()) {
            ProductoDto prod = productos.get(i.getProductoId());
            if (prod == null || !Boolean.TRUE.equals(prod.activo())) {
                throw new ReglaNegocioException("El producto " + i.getNombre() + " ya no está disponible");
            }
            p.getItems().add(new ItemPedido(null, prod.id(), prod.nombre(), prod.precioKg(), i.getKg()));
        }

        // 2) Datos de entrega y totales
        p.setCodigo("TMP-" + UUID.randomUUID().toString().substring(0, 12));
        p.setClienteId(req.clienteId());
        p.setNombreCliente(req.nombreCliente().trim());
        p.setEmail(req.email().trim().toLowerCase());
        p.setTelefono(vacioANull(req.telefono()));
        p.setDireccion(req.direccion().trim());
        p.setComuna(req.comuna().trim());
        p.setReferencia(vacioANull(req.referencia()));
        p.setComentario(vacioANull(req.comentario()));
        p.setFechaEntrega(req.fechaEntrega());
        p.setHorario(req.horario());
        p.setMetodoPago(req.metodoPago());
        p.setEnvio(config.tarifa(p.getComuna()));
        p.recalcularTotal();

        // 3) Cupón
        if (req.codigoCupon() != null && !req.codigoCupon().isBlank()) {
            String codigo = req.codigoCupon().trim().toUpperCase();
            CuponDto cupon = marketing.validar(codigo, req.clienteId(), p.getSubtotal());
            if (!Boolean.TRUE.equals(cupon.valido())) {
                throw new SolicitudInvalidaException(cupon.mensaje() == null ? "Cupón no válido" : cupon.mensaje());
            }
            p.setCodigoCupon(codigo);
            p.setDescuento(cupon.descuento().min(p.getSubtotal()));
            p.recalcularTotal();
        }

        // 4) Guardar con su estado inicial
        Instant ahora = Instant.now();
        p.setEstado(EstadoPedido.PENDIENTE_PAGO);
        p.setCreadoEn(ahora);
        p.setActualizadoEn(ahora);
        p.getHistorial().add(new HistorialEstado(null, EstadoPedido.PENDIENTE_PAGO, "Pedido creado", null, ahora, false));
        p = repository.save(p);
        p.setCodigo(String.format("BP-%06d", 100000 + p.getId()));

        // 5) Reserva de stock (si no alcanza, ms-inventario responde 409 y todo se deshace)
        inventario.reservar(p.getId(), itemsReserva(p));

        // 6) Efectos secundarios
        if (p.getCodigoCupon() != null) {
            marketing.registrarUso(p.getCodigoCupon(), p.getId(), p.getClienteId());
        }
        carrito.getItems().clear();
        carrito.recalcular();
        carritoRepository.save(carrito);
        Map<String, Object> vars = variables(p);
        vars.put("total", p.getTotal());
        vars.put("fechaEntrega", p.getFechaEntrega().toString());
        vars.put("horario", p.getHorario());
        notificaciones.enviar("PEDIDO_CONFIRMADO", "CORREO", p.getEmail(), vars);
        return p;
    }

    /* =============== Consultas =============== */

    /** CU-05.05 Consultar pedidos con filtros (panel). Las fechas filtran por día de creación. */
    public List<Pedido> listar(EstadoPedido estado, Long clienteId, List<String> comunas, LocalDate desde,
            LocalDate hasta, LocalDate fechaEntrega, Long repartidorId, String q) {
        ZoneId zona = ZoneId.systemDefault();
        String filtro = q == null ? "" : q.trim().toLowerCase();
        return repository.findAll().stream()
                .filter(p -> estado == null || p.getEstado() == estado)
                .filter(p -> clienteId == null || clienteId.equals(p.getClienteId()))
                .filter(p -> comunas == null || comunas.isEmpty()
                        || comunas.stream().anyMatch(c -> c.equalsIgnoreCase(p.getComuna())))
                .filter(p -> desde == null || !p.getCreadoEn().atZone(zona).toLocalDate().isBefore(desde))
                .filter(p -> hasta == null || !p.getCreadoEn().atZone(zona).toLocalDate().isAfter(hasta))
                .filter(p -> fechaEntrega == null || fechaEntrega.equals(p.getFechaEntrega()))
                .filter(p -> repartidorId == null || repartidorId.equals(p.getRepartidorId()))
                .filter(p -> filtro.isEmpty() || p.getCodigo().toLowerCase().contains(filtro)
                        || p.getNombreCliente().toLowerCase().contains(filtro)
                        || p.getEmail().toLowerCase().contains(filtro))
                .sorted(Comparator.comparing(Pedido::getCreadoEn).reversed())
                .toList();
    }

    public Pedido obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido " + id));
    }

    public Pedido porCodigo(String codigo) {
        return repository.findByCodigoIgnoreCase(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido " + codigo));
    }

    /** CU-05.04 Mis pedidos. */
    public List<Pedido> delCliente(Long clienteId) {
        return repository.findByClienteIdOrderByCreadoEnDesc(clienteId);
    }

    public SeguimientoResponse seguimiento(Long id) {
        Pedido p = obtener(id);
        Map<EstadoPedido, Instant> fechas = new HashMap<>();
        p.getHistorial().stream().filter(h -> !Boolean.TRUE.equals(h.getEsReversion()))
                .forEach(h -> fechas.put(h.getEstado(), h.getFecha()));
        int actual = FLUJO.indexOf(p.getEstado());
        List<PasoSeguimiento> pasos = new ArrayList<>();
        for (int i = 0; i < FLUJO.size(); i++) {
            EstadoPedido e = FLUJO.get(i);
            boolean completado = actual >= 0 && i <= actual;
            pasos.add(new PasoSeguimiento(e, completado, completado ? fechas.get(e) : null));
        }
        return new SeguimientoResponse(p.getId(), p.getCodigo(), p.getEstado(), p.getFechaEntrega(), p.getHorario(),
                pasos, p.getHistorial());
    }

    public Set<EstadoPedido> transicionesDe(Long id) {
        return TRANSICIONES.get(obtener(id).getEstado());
    }

    /* =============== Estados =============== */

    /** CU-05.06 Cambiar estado (solo transiciones válidas hacia adelante). */
    @Transactional
    public Pedido cambiarEstado(Long id, EstadoPedido nuevo, String nota, Long usuarioId) {
        Pedido p = obtener(id);
        EstadoPedido anterior = p.getEstado();
        if (!TRANSICIONES.get(anterior).contains(nuevo)) {
            throw new ReglaNegocioException("No se puede pasar de " + anterior + " a " + nuevo
                    + ". Permitidos: " + TRANSICIONES.get(anterior));
        }
        aplicarEfectos(p, anterior, nuevo);
        registrar(p, nuevo, nota, usuarioId, false);
        return repository.save(p);
    }

    /** Estados en que el admin puede asignar (o cambiar) el repartidor. */
    public static final Set<EstadoPedido> ASIGNABLES = EnumSet.of(EstadoPedido.PAGADO, EstadoPedido.EN_PREPARACION,
            EstadoPedido.ENVIADO);

    @Transactional
    public Pedido asignarRepartidor(Long id, Long repartidorId, String repartidorNombre, Long usuarioId) {
        Pedido p = obtener(id);
        boolean contraEntregaPendiente = p.getEstado() == EstadoPedido.PENDIENTE_PAGO
                && p.getMetodoPago() == MetodoPago.CONTRA_ENTREGA;
        if (!ASIGNABLES.contains(p.getEstado()) && !contraEntregaPendiente) {
            throw new ReglaNegocioException("Solo se asigna repartidor a pedidos pagados, en preparación o enviados (este está "
                    + p.getEstado() + ")");
        }
        String nombre = repartidorNombre == null || repartidorNombre.isBlank() ? "repartidor " + repartidorId
                : repartidorNombre.trim();
        boolean reasignacion = p.getRepartidorId() != null && !p.getRepartidorId().equals(repartidorId);
        p.setRepartidorId(repartidorId);
        p.setRepartidorNombre(nombre);
        p.setAsignadoEn(Instant.now());
        String nota = (reasignacion ? "Reasignado a " : "Asignado a ") + nombre;
        if (p.getEstado() == EstadoPedido.PAGADO || contraEntregaPendiente) {
            aplicarEfectos(p, p.getEstado(), EstadoPedido.EN_PREPARACION);
            registrar(p, EstadoPedido.EN_PREPARACION, nota, usuarioId, false);
        } else {
            p.setActualizadoEn(Instant.now());
        }
        p.getNotas().add(new NotaInterna(null, nota, usuarioId, Instant.now()));
        return repository.save(p);
    }

    /** CU-05.06 Retroceder al estado anterior del historial. */
    @Transactional
    public Pedido retroceder(Long id, String motivo, Long usuarioId) {
        Pedido p = obtener(id);
        Deque<EstadoPedido> pila = new ArrayDeque<>();
        for (HistorialEstado h : p.getHistorial()) {
            if (Boolean.TRUE.equals(h.getEsReversion())) {
                if (pila.size() > 1) {
                    pila.pop();
                }
            } else {
                pila.push(h.getEstado());
            }
        }
        if (pila.size() < 2) {
            throw new ReglaNegocioException("El pedido no tiene un estado anterior al cual volver");
        }
        EstadoPedido actual = pila.pop();
        EstadoPedido destino = pila.peek();
        boolean veniaDeAnulado = actual == EstadoPedido.CANCELADO || actual == EstadoPedido.REEMBOLSADO;
        if (veniaDeAnulado && destino != EstadoPedido.CANCELADO && destino != EstadoPedido.REEMBOLSADO) {
            // hay que volver a apartar el stock que se liberó al anular
            inventario.reservar(p.getId(), itemsReserva(p));
            if (CON_STOCK_DESCONTADO.contains(destino)) {
                inventario.confirmar(p.getId());
            }
        }
        registrar(p, destino, "Retroceso: " + motivo, usuarioId, true);
        return repository.save(p);
    }

    /** CU-05.09 Cancelar pedido (cliente o personal). */
    @Transactional
    public Pedido cancelar(Long id, String motivo, Long usuarioId) {
        return cambiarEstado(id, EstadoPedido.CANCELADO, motivo, usuarioId);
    }

    /** Eventos que envía ms-pagos. */
    @Transactional
    public Pedido registrarPago(Long id, String evento, Long pagoId, String detalle) {
        Pedido p = obtener(id);
        if (pagoId != null) {
            p.setPagoId(pagoId);
        }
        switch (evento.toUpperCase()) {
            case "APROBADO" -> {
                if (p.getEstado() == EstadoPedido.PENDIENTE_PAGO) {
                    aplicarEfectos(p, p.getEstado(), EstadoPedido.PAGADO);
                    registrar(p, EstadoPedido.PAGADO, "Pago aprobado" + sufijo(detalle), null, false);
                } else if (p.getEstado() == EstadoPedido.CANCELADO) {
                    p.getNotas().add(0, new NotaInterna(null, "Llegó un pago aprobado para un pedido cancelado: revisar reembolso",
                            null, Instant.now()));
                }
            }
            case "CONTRA_ENTREGA" -> {
                if (p.getEstado() == EstadoPedido.PENDIENTE_PAGO) {
                    aplicarEfectos(p, p.getEstado(), EstadoPedido.EN_PREPARACION);
                    registrar(p, EstadoPedido.EN_PREPARACION, "Pago contra entrega" + sufijo(detalle), null, false);
                }
            }
            case "RECHAZADO" -> p.getHistorial().add(new HistorialEstado(null, p.getEstado(),
                    "Pago rechazado" + sufijo(detalle), null, Instant.now(), false));
            case "REEMBOLSADO" -> {
                if (p.getEstado() != EstadoPedido.REEMBOLSADO) {
                    aplicarEfectos(p, p.getEstado(), EstadoPedido.REEMBOLSADO);
                    registrar(p, EstadoPedido.REEMBOLSADO, "Reembolso aprobado" + sufijo(detalle), null, false);
                }
            }
            default -> throw new SolicitudInvalidaException("Evento de pago desconocido: " + evento);
        }
        return repository.save(p);
    }

    /** CU-05.10 Cancela pedidos sin pago pasado el tiempo de reserva (Webpay/tarjeta) o 24 h (transferencia). */
    @Transactional
    public int cancelarVencidos() {
        long minutos = config.regla("RESERVA_MIN", BigDecimal.valueOf(30)).longValue();
        Instant limiteOnline = Instant.now().minus(minutos, ChronoUnit.MINUTES);
        Instant limiteTransferencia = Instant.now().minus(24, ChronoUnit.HOURS);
        int n = 0;
        for (Pedido p : repository.findByEstadoAndCreadoEnBefore(EstadoPedido.PENDIENTE_PAGO, limiteOnline)) {
            boolean vencido = switch (p.getMetodoPago()) {
                case WEBPAY, TARJETA -> true;
                case TRANSFERENCIA -> p.getCreadoEn().isBefore(limiteTransferencia);
                case CONTRA_ENTREGA -> false;
            };
            if (vencido) {
                aplicarEfectos(p, p.getEstado(), EstadoPedido.CANCELADO);
                registrar(p, EstadoPedido.CANCELADO, "Cancelado automáticamente: no se pagó a tiempo", null, false);
                repository.save(p);
                n++;
            }
        }
        return n;
    }

    /* =============== Notas y etiqueta =============== */

    public List<NotaInterna> notas(Long id) {
        return obtener(id).getNotas();
    }

    @Transactional
    public NotaInterna agregarNota(Long id, String texto, Long autorId) {
        Pedido p = obtener(id);
        NotaInterna n = new NotaInterna(null, texto.trim(), autorId, Instant.now());
        p.getNotas().add(0, n);
        repository.flush();
        return n;
    }

    /** CU-05.08 Datos para imprimir la etiqueta de envío. */
    public EtiquetaResponse etiqueta(Long id) {
        Pedido p = obtener(id);
        boolean cobrar = p.getMetodoPago() == MetodoPago.CONTRA_ENTREGA && p.getEstado() != EstadoPedido.ENTREGADO;
        return new EtiquetaResponse(p.getCodigo(), p.getNombreCliente(), p.getTelefono(), p.getDireccion(),
                p.getComuna(), p.getReferencia(), p.getFechaEntrega(), p.getHorario(), p.getTotalKg(),
                p.getItems().size(), cobrar, cobrar ? p.getTotal() : BigDecimal.ZERO,
                "Producto congelado: mantener a -18 °C o menos");
    }

    /* =============== internos =============== */

    /** Efectos en otros microservicios al cambiar de estado. */
    private void aplicarEfectos(Pedido p, EstadoPedido anterior, EstadoPedido nuevo) {
        switch (nuevo) {
            case PAGADO, EN_PREPARACION -> {
                if (anterior == EstadoPedido.PENDIENTE_PAGO) {
                    confirmarStock(p);
                }
            }
            case CANCELADO -> inventario.liberar(p.getId());
            case REEMBOLSADO -> {
                if (anterior != EstadoPedido.ENTREGADO && anterior != EstadoPedido.CANCELADO) {
                    inventario.liberar(p.getId());
                }
            }
            case ENTREGADO -> clientes.registrarCompra(p.getClienteId(), p.getTotal(), p.getId());
            default -> {
                // ENVIADO y PENDIENTE_PAGO no tienen efectos extra
            }
        }
        Map<String, Object> vars = variables(p);
        vars.put("estadoAnterior", anterior.name());
        vars.put("estado", nuevo.name());
        notificaciones.enviar("CAMBIO_ESTADO", "CORREO", p.getEmail(), vars);
    }

    /** Descuenta el stock reservado. Si la reserva expiró, intenta reservar de nuevo antes de confirmar. */
    private void confirmarStock(Pedido p) {
        try {
            inventario.confirmar(p.getId());
            return;
        } catch (ReglaNegocioException e) {
            log.info("La reserva del pedido {} ya no estaba activa; se intenta reservar de nuevo", p.getCodigo());
        } catch (RuntimeException e) {
            anotarProblemaStock(p, e);
            return;
        }
        try {
            inventario.reservar(p.getId(), itemsReserva(p));
            inventario.confirmar(p.getId());
        } catch (RuntimeException e2) {
            anotarProblemaStock(p, e2);
        }
    }

    private void anotarProblemaStock(Pedido p, RuntimeException e) {
        log.warn("No se pudo descontar stock del pedido {}: {}", p.getCodigo(), e.getMessage());
        p.getNotas().add(0, new NotaInterna(null, "Atención: no se pudo descontar stock (" + e.getMessage()
                + "). Revisar inventario.", null, Instant.now()));
    }

    private void registrar(Pedido p, EstadoPedido estado, String nota, Long usuarioId, boolean reversion) {
        Instant ahora = Instant.now();
        p.setEstado(estado);
        p.setActualizadoEn(ahora);
        p.getHistorial().add(new HistorialEstado(null, estado, nota, usuarioId, ahora, reversion));
    }

    private void validarEntrega(LocalDate fecha, String horario) {
        LocalDate hoy = LocalDate.now();
        long maxDias = config.regla("DIAS_MAX_ENTREGA", BigDecimal.valueOf(14)).longValue();
        if (!fecha.isAfter(hoy) || fecha.isAfter(hoy.plusDays(maxDias))) {
            throw new SolicitudInvalidaException("Elige una fecha de entrega entre mañana y dentro de " + maxDias + " días");
        }
        if (!PedidoDtos.HORARIOS.contains(horario)) {
            throw new SolicitudInvalidaException("Horario inválido. Opciones: " + PedidoDtos.HORARIOS);
        }
    }

    private List<Map<String, Object>> itemsReserva(Pedido p) {
        Map<Long, BigDecimal> porProducto = new LinkedHashMap<>();
        p.getItems().forEach(i -> porProducto.merge(i.getProductoId(), i.getKg(), BigDecimal::add));
        List<Map<String, Object>> items = new ArrayList<>();
        porProducto.forEach((productoId, kg) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("productoId", productoId);
            m.put("kg", kg);
            items.add(m);
        });
        return items;
    }

    private Map<String, Object> variables(Pedido p) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("codigo", p.getCodigo());
        vars.put("cliente", p.getNombreCliente());
        vars.put("pedidoId", p.getId());
        return vars;
    }

    private static String sufijo(String detalle) {
        return detalle == null || detalle.isBlank() ? "" : " (" + detalle + ")";
    }

    private static String vacioANull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
