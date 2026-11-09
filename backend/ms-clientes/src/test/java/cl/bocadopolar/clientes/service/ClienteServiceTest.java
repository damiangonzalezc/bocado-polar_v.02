package cl.bocadopolar.clientes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import cl.bocadopolar.clientes.client.ConfigClient;
import cl.bocadopolar.clientes.model.Cliente;
import cl.bocadopolar.clientes.model.SegmentoCliente;
import cl.bocadopolar.clientes.repository.ClienteRepository;

class ClienteServiceTest {

    @Mock
    private ClienteRepository repository;

    @Mock
    private ConfigClient configClient;

    @InjectMocks
    private ClienteService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(configClient.regla(eq("VIP_MONTO"), any())).thenReturn(new BigDecimal("500000"));
        when(configClient.regla(eq("FRECUENTE_PEDIDOS"), any())).thenReturn(new BigDecimal("3"));
    }

    @Test
    void compraGrandeLoVuelveVip() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setGastoTotal(new BigDecimal("450000"));
        c.setTotalPedidos(1);
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        when(repository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        Cliente r = service.registrarCompra(1L, new BigDecimal("60000"));

        assertThat(r.getTotalPedidos()).isEqualTo(2);
        assertThat(r.getSegmento()).isEqualTo(SegmentoCliente.VIP);
    }

    @Test
    void tresPedidosLoVuelvenFrecuente() {
        Cliente c = new Cliente();
        c.setGastoTotal(new BigDecimal("90000"));
        c.setTotalPedidos(3);
        assertThat(service.calcularSegmento(c)).isEqualTo(SegmentoCliente.FRECUENTE);
    }
}
