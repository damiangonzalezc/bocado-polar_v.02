package cl.bocadopolar.pedidos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import cl.bocadopolar.pedidos.client.Clientes.CatalogoClient;
import cl.bocadopolar.pedidos.client.Clientes.ClientesClient;
import cl.bocadopolar.pedidos.client.Clientes.ConfigClient;
import cl.bocadopolar.pedidos.client.Clientes.InventarioClient;
import cl.bocadopolar.pedidos.client.Clientes.MarketingClient;
import cl.bocadopolar.pedidos.client.Clientes.NotificacionesClient;
import cl.bocadopolar.pedidos.exception.ReglaNegocioException;
import cl.bocadopolar.pedidos.model.EstadoPedido;
import cl.bocadopolar.pedidos.model.HistorialEstado;
import cl.bocadopolar.pedidos.model.MetodoPago;
import cl.bocadopolar.pedidos.model.Pedido;
import cl.bocadopolar.pedidos.repository.CarritoRepository;
import cl.bocadopolar.pedidos.repository.PedidoRepository;

class PedidoServiceTest {

    @Mock private PedidoRepository repository;
    @Mock private CarritoRepository carritoRepository;
    @Mock private CatalogoClient catalogo;
    @Mock private ConfigClient config;
    @Mock private MarketingClient marketing;
    @Mock private InventarioClient inventario;
    @Mock private ClientesClient clientes;
    @Mock private NotificacionesClient notificaciones;

    @InjectMocks
    private PedidoService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(repository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Pedido pedido(EstadoPedido... estados) {
        Pedido p = new Pedido();
        p.setId(1L);
        p.setCodigo("BP-100001");
        p.setClienteId(1L);
        p.setEmail("cliente@bocado.cl");
        p.setNombreCliente("Cliente Demo");
        p.setMetodoPago(MetodoPago.WEBPAY);
        p.setTotal(new BigDecimal("48455"));
        for (EstadoPedido e : estados) {
            p.getHistorial().add(new HistorialEstado(null, e, null, null, Instant.now(), false));
            p.setEstado(e);
        }
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void noSePuedeSaltarDePendienteAEntregado() {
        pedido(EstadoPedido.PENDIENTE_PAGO);
        assertThatThrownBy(() -> service.cambiarEstado(1L, EstadoPedido.ENTREGADO, null, 1L))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void cancelarLiberaElStock() {
        pedido(EstadoPedido.PENDIENTE_PAGO);
        Pedido r = service.cancelar(1L, "Cliente se arrepintió", 1L);
        assertThat(r.getEstado()).isEqualTo(EstadoPedido.CANCELADO);
        verify(inventario).liberar(1L);
    }

    @Test
    void retrocederVuelveAlEstadoAnteriorYQuedaEnElHistorial() {
        pedido(EstadoPedido.PENDIENTE_PAGO, EstadoPedido.PAGADO, EstadoPedido.EN_PREPARACION);
        Pedido r = service.retroceder(1L, "Error de digitación", 1L);
        assertThat(r.getEstado()).isEqualTo(EstadoPedido.PAGADO);
        assertThat(r.getHistorial()).hasSize(4);
        assertThat(r.getHistorial().get(3).getEsReversion()).isTrue();
    }

    @Test
    void entregadoRegistraLaCompraDelCliente() {
        pedido(EstadoPedido.PENDIENTE_PAGO, EstadoPedido.PAGADO, EstadoPedido.EN_PREPARACION, EstadoPedido.ENVIADO);
        service.cambiarEstado(1L, EstadoPedido.ENTREGADO, null, 5L);
        verify(clientes).registrarCompra(1L, new BigDecimal("48455"), 1L);
    }
}
