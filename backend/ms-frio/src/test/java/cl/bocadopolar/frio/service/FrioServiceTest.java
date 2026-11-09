package cl.bocadopolar.frio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import cl.bocadopolar.frio.client.ConfigClient;
import cl.bocadopolar.frio.client.InventarioClient;
import cl.bocadopolar.frio.model.AlertaFrio;
import cl.bocadopolar.frio.model.OrigenTemp;
import cl.bocadopolar.frio.model.RegistroTemperatura;
import cl.bocadopolar.frio.model.Severidad;
import cl.bocadopolar.frio.repository.AlertaFrioRepository;
import cl.bocadopolar.frio.repository.CuentaRegresivaRutaRepository;
import cl.bocadopolar.frio.repository.RegistroTemperaturaRepository;

class FrioServiceTest {

    @Mock private RegistroTemperaturaRepository registroRepository;
    @Mock private AlertaFrioRepository alertaRepository;
    @Mock private CuentaRegresivaRutaRepository cuentaRepository;
    @Mock private ConfigClient config;
    @Mock private InventarioClient inventario;

    @InjectMocks
    private FrioService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(config.regla(eq("TEMP_MAX_ALERTA"), any())).thenReturn(new BigDecimal("-15"));
        when(config.regla(eq("MIN_SOSTENIDO_CRITICO"), any())).thenReturn(new BigDecimal("30"));
        when(alertaRepository.save(any(AlertaFrio.class))).thenAnswer(i -> i.getArgument(0));
        when(alertaRepository.findByOrigenAndReferenciaIdAndAtendidaFalse(any(), anyLong())).thenReturn(List.of());
    }

    @Test
    void temperaturaCorrectaNoGeneraAlerta() {
        RegistroTemperatura r = new RegistroTemperatura(1L, OrigenTemp.LOTE, 3L, -18.0, Instant.now(), 3L);
        assertThat(service.evaluarUmbral(r)).isNull();
    }

    @Test
    void sobreMenos15GeneraAdvertencia() {
        RegistroTemperatura r = new RegistroTemperatura(1L, OrigenTemp.LOTE, 3L, -14.0, Instant.now(), 3L);
        when(registroRepository.findByOrigenAndReferenciaIdOrderByFechaDesc(OrigenTemp.LOTE, 3L)).thenReturn(List.of(r));
        AlertaFrio a = service.evaluarUmbral(r);
        assertThat(a.getSeveridad()).isEqualTo(Severidad.ADVERTENCIA);
        verify(inventario, never()).bloquearLote(anyLong(), anyString());
    }

    @Test
    void sostenidoMasDe30MinutosEsCriticaYBloqueaElLote() {
        Instant ahora = Instant.now();
        RegistroTemperatura actual = new RegistroTemperatura(2L, OrigenTemp.LOTE, 3L, -13.0, ahora, 3L);
        RegistroTemperatura antes = new RegistroTemperatura(1L, OrigenTemp.LOTE, 3L, -14.0, ahora.minus(40, ChronoUnit.MINUTES), 3L);
        when(registroRepository.findByOrigenAndReferenciaIdOrderByFechaDesc(OrigenTemp.LOTE, 3L)).thenReturn(List.of(actual, antes));
        AlertaFrio a = service.evaluarUmbral(actual);
        assertThat(a.getSeveridad()).isEqualTo(Severidad.CRITICA);
        verify(inventario).bloquearLote(eq(3L), anyString());
    }
}
