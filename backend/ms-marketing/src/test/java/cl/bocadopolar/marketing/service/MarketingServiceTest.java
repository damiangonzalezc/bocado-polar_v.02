package cl.bocadopolar.marketing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import cl.bocadopolar.marketing.client.Clientes.ClientesClient;
import cl.bocadopolar.marketing.client.Clientes.NotificacionesClient;
import cl.bocadopolar.marketing.client.Clientes.PedidosClient;
import cl.bocadopolar.marketing.dto.MarketingDtos.ValidacionCupon;
import cl.bocadopolar.marketing.model.Cupon;
import cl.bocadopolar.marketing.model.TipoDescuento;
import cl.bocadopolar.marketing.repository.CanalCampanaRepository;
import cl.bocadopolar.marketing.repository.CarritoAbandonadoRepository;
import cl.bocadopolar.marketing.repository.CuponRepository;
import cl.bocadopolar.marketing.repository.UsoCuponRepository;

class MarketingServiceTest {

    @Mock private CuponRepository cuponRepository;
    @Mock private UsoCuponRepository usoRepository;
    @Mock private CanalCampanaRepository canalRepository;
    @Mock private CarritoAbandonadoRepository abandonadoRepository;
    @Mock private PedidosClient pedidos;
    @Mock private ClientesClient clientes;
    @Mock private NotificacionesClient notificaciones;

    @InjectMocks
    private MarketingService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Cupon cupon(TipoDescuento tipo, int valor, int diasVigencia) {
        Cupon c = new Cupon();
        c.setId(1L);
        c.setCodigo("BIENVENIDA10");
        c.setTipo(tipo);
        c.setValor(BigDecimal.valueOf(valor));
        c.setCompraMinima(BigDecimal.ZERO);
        c.setLimiteUsos(100);
        c.setUsos(0);
        c.setLimitePorCliente(1);
        c.setActivo(true);
        c.setVence(LocalDate.now().plusDays(diasVigencia));
        return c;
    }

    @Test
    void cuponPorcentajeCalculaDescuento() {
        when(cuponRepository.findByCodigoIgnoreCase("BIENVENIDA10")).thenReturn(Optional.of(cupon(TipoDescuento.PORCENTAJE, 10, 30)));
        ValidacionCupon v = service.validar("bienvenida10", 1L, new BigDecimal("45000"));
        assertThat(v.valido()).isTrue();
        assertThat(v.descuento()).isEqualByComparingTo("4500");
    }

    @Test
    void cuponVencidoNoEsValido() {
        when(cuponRepository.findByCodigoIgnoreCase("BIENVENIDA10")).thenReturn(Optional.of(cupon(TipoDescuento.PORCENTAJE, 10, -1)));
        assertThat(service.validar("BIENVENIDA10", 1L, new BigDecimal("45000")).valido()).isFalse();
    }

    @Test
    void montoFijoNoSuperaElSubtotal() {
        assertThat(cupon(TipoDescuento.MONTO_FIJO, 5000, 10).calcularDescuento(new BigDecimal("3000")))
                .isEqualByComparingTo("3000");
    }
}
