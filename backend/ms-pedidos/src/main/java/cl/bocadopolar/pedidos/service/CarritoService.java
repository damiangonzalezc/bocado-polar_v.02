package cl.bocadopolar.pedidos.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bocadopolar.pedidos.client.Clientes.CatalogoClient;
import cl.bocadopolar.pedidos.client.Clientes.ConfigClient;
import cl.bocadopolar.pedidos.client.Clientes.ProductoDto;
import cl.bocadopolar.pedidos.dto.PedidoDtos.CarritoRequest;
import cl.bocadopolar.pedidos.exception.RecursoNoEncontradoException;
import cl.bocadopolar.pedidos.exception.ReglaNegocioException;
import cl.bocadopolar.pedidos.exception.SolicitudInvalidaException;
import cl.bocadopolar.pedidos.model.Carrito;
import cl.bocadopolar.pedidos.model.ItemCarrito;
import cl.bocadopolar.pedidos.model.ItemPedido;
import cl.bocadopolar.pedidos.model.Pedido;
import cl.bocadopolar.pedidos.repository.CarritoRepository;
import cl.bocadopolar.pedidos.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;

/** CU-05.01 Gestionar carrito (en kg, máximo 12,5 kg por producto/formato — RN-02). */
@Service
@RequiredArgsConstructor
public class CarritoService {

    private static final BigDecimal MEDIO_KG = new BigDecimal("0.5");

    private final CarritoRepository repository;
    private final PedidoRepository pedidoRepository;
    private final CatalogoClient catalogo;
    private final ConfigClient config;

    @Transactional
    public Carrito obtenerOCrear(CarritoRequest req) {
        Optional<Carrito> existente = Optional.empty();
        if (req != null && req.clienteId() != null) {
            existente = repository.findByClienteId(req.clienteId());
        } else if (req != null && req.sesionAnonimaId() != null && !req.sesionAnonimaId().isBlank()) {
            existente = repository.findBySesionAnonimaId(req.sesionAnonimaId());
        }
        if (existente.isPresent()) {
            return existente.get();
        }
        Carrito c = new Carrito();
        if (req != null && req.clienteId() != null) {
            c.setClienteId(req.clienteId());
        } else {
            c.setSesionAnonimaId(req != null && req.sesionAnonimaId() != null && !req.sesionAnonimaId().isBlank()
                    ? req.sesionAnonimaId() : UUID.randomUUID().toString());
        }
        c.setCreadoEn(Instant.now());
        c.recalcular();
        return repository.save(c);
    }

    public Carrito obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el carrito " + id));
    }

    @Transactional
    public Carrito delCliente(Long clienteId) {
        return obtenerOCrear(new CarritoRequest(clienteId, null));
    }

    /** Suma kg al producto (si ya estaba) o lo agrega. */
    @Transactional
    public Carrito agregar(Long carritoId, Long productoId, BigDecimal kg) {
        Carrito c = obtener(carritoId);
        validarPaso(kg);
        ItemCarrito item = buscar(c, productoId).orElse(null);
        BigDecimal nuevoKg = item == null ? kg : item.getKg().add(kg);
        fijarInterno(c, productoId, nuevoKg);
        return repository.save(c);
    }

    /** Fija la cantidad exacta; 0 kg quita el producto. */
    @Transactional
    public Carrito fijar(Long carritoId, Long productoId, BigDecimal kg) {
        Carrito c = obtener(carritoId);
        if (kg.signum() == 0) {
            c.getItems().removeIf(i -> i.getProductoId().equals(productoId));
            c.recalcular();
            return repository.save(c);
        }
        validarPaso(kg);
        fijarInterno(c, productoId, kg);
        return repository.save(c);
    }

    @Transactional
    public Carrito quitar(Long carritoId, Long productoId) {
        Carrito c = obtener(carritoId);
        if (!c.getItems().removeIf(i -> i.getProductoId().equals(productoId))) {
            throw new RecursoNoEncontradoException("El producto " + productoId + " no está en el carrito");
        }
        c.recalcular();
        return repository.save(c);
    }

    @Transactional
    public Carrito vaciar(Long carritoId) {
        Carrito c = obtener(carritoId);
        c.getItems().clear();
        c.recalcular();
        return repository.save(c);
    }

    /** Al iniciar sesión: pasa el carrito del visitante al cliente (si el cliente ya tenía uno, se juntan). */
    @Transactional
    public Carrito asociarCliente(Long carritoId, Long clienteId) {
        Carrito anonimo = obtener(carritoId);
        if (clienteId.equals(anonimo.getClienteId())) {
            return anonimo;
        }
        Optional<Carrito> delCliente = repository.findByClienteId(clienteId);
        if (delCliente.isEmpty()) {
            anonimo.setClienteId(clienteId);
            anonimo.setSesionAnonimaId(null);
            anonimo.recalcular();
            return repository.save(anonimo);
        }
        Carrito destino = delCliente.get();
        for (ItemCarrito i : anonimo.getItems()) {
            BigDecimal kg = buscar(destino, i.getProductoId()).map(x -> x.getKg().add(i.getKg())).orElse(i.getKg());
            BigDecimal max = maxKg();
            fijarInterno(destino, i.getProductoId(), kg.min(max));
        }
        repository.delete(anonimo);
        return repository.save(destino);
    }

    /** CU-02.05 Volver a pedir: copia los productos de un pedido anterior al carrito. */
    @Transactional
    public Carrito volverAPedir(Long carritoId, Long pedidoId) {
        Carrito c = obtener(carritoId);
        Pedido p = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido " + pedidoId));
        if (c.getClienteId() != null && !c.getClienteId().equals(p.getClienteId())) {
            throw new ReglaNegocioException("El pedido no pertenece al cliente del carrito");
        }
        BigDecimal max = maxKg();
        for (ItemPedido i : p.getItems()) {
            BigDecimal kg = buscar(c, i.getProductoId()).map(x -> x.getKg().add(i.getKg())).orElse(i.getKg());
            fijarInterno(c, i.getProductoId(), kg.min(max));
        }
        return repository.save(c);
    }

    /** Carritos con productos sin movimiento hace N horas (los usa ms-marketing para el rescate). */
    public List<Carrito> inactivos(int horas) {
        Instant limite = Instant.now().minus(horas, ChronoUnit.HOURS);
        return repository.findByActualizadoEnBefore(limite).stream()
                .filter(c -> !c.getItems().isEmpty())
                .toList();
    }

    /* ---------- internos ---------- */

    private void fijarInterno(Carrito c, Long productoId, BigDecimal kg) {
        BigDecimal max = maxKg();
        if (kg.compareTo(max) > 0) {
            throw new ReglaNegocioException("Máximo " + max.stripTrailingZeros().toPlainString()
                    + " kg por formato (RN-02)");
        }
        ProductoDto p = catalogo.producto(productoId);
        if (p == null || !Boolean.TRUE.equals(p.activo())) {
            throw new ReglaNegocioException("El producto " + productoId + " no está disponible");
        }
        ItemCarrito item = buscar(c, productoId).orElse(null);
        if (item == null) {
            item = new ItemCarrito(null, productoId, p.nombre(), p.imagen(), kg, p.precioKg());
            c.getItems().add(item);
        } else {
            item.setKg(kg);
            item.setNombre(p.nombre());
            item.setImagen(p.imagen());
            item.setPrecioKg(p.precioKg());
        }
        c.recalcular();
    }

    private Optional<ItemCarrito> buscar(Carrito c, Long productoId) {
        return c.getItems().stream().filter(i -> i.getProductoId().equals(productoId)).findFirst();
    }

    private void validarPaso(BigDecimal kg) {
        if (kg.compareTo(MEDIO_KG) < 0 || kg.remainder(MEDIO_KG).signum() != 0) {
            throw new SolicitudInvalidaException("Las cantidades van en pasos de 0,5 kg");
        }
    }

    private BigDecimal maxKg() {
        return config.regla("MAX_KG_FORMATO", new BigDecimal("12.5"));
    }
}
