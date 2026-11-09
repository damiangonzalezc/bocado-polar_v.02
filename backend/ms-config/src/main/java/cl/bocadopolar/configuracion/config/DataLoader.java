package cl.bocadopolar.configuracion.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cl.bocadopolar.configuracion.model.ConfiguracionGlobal;
import cl.bocadopolar.configuracion.model.EmpresaLogistica;
import cl.bocadopolar.configuracion.model.PasarelaPago;
import cl.bocadopolar.configuracion.model.ReglaNegocio;
import cl.bocadopolar.configuracion.model.ZonaEnvio;
import cl.bocadopolar.configuracion.repository.ConfiguracionGlobalRepository;
import cl.bocadopolar.configuracion.repository.EmpresaLogisticaRepository;
import cl.bocadopolar.configuracion.repository.PasarelaPagoRepository;
import cl.bocadopolar.configuracion.repository.ReglaNegocioRepository;
import cl.bocadopolar.configuracion.repository.ZonaEnvioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Carga los datos del prototipo la primera vez (tablas vacías). */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bocado.semilla.habilitada", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {

    private final ConfiguracionGlobalRepository configuracionRepository;
    private final ZonaEnvioRepository zonaRepository;
    private final EmpresaLogisticaRepository empresaRepository;
    private final PasarelaPagoRepository pasarelaRepository;
    private final ReglaNegocioRepository reglaRepository;

    @Override
    public void run(String... args) {
        LocalDate hoy = LocalDate.now();
        if (configuracionRepository.count() == 0) {
            configuracionRepository.save(new ConfiguracionGlobal(null, "Chile", "CLP", true, new BigDecimal("19.00"), hoy));
        }
        if (zonaRepository.count() == 0) {
            zonaRepository.saveAll(List.of(
                    new ZonaEnvio(null, "Concepción", new BigDecimal("2990"), true),
                    new ZonaEnvio(null, "Chiguayante", new BigDecimal("3490"), true),
                    new ZonaEnvio(null, "Tomé", new BigDecimal("4490"), true),
                    new ZonaEnvio(null, "Penco", new BigDecimal("4490"), true)));
        }
        if (empresaRepository.count() == 0) {
            empresaRepository.saveAll(List.of(
                    new EmpresaLogistica(null, "Flota propia BOCADO", "Camión refrigerado", true),
                    new EmpresaLogistica(null, "Starken Frío (convenio)", "Courier con cadena de frío", false)));
        }
        if (pasarelaRepository.count() == 0) {
            pasarelaRepository.saveAll(List.of(
                    new PasarelaPago(null, "Webpay", true),
                    new PasarelaPago(null, "Mercado Pago", false),
                    new PasarelaPago(null, "Stripe", false),
                    new PasarelaPago(null, "PayPal", false),
                    new PasarelaPago(null, "Transferencia bancaria", true)));
        }
        if (reglaRepository.count() == 0) {
            reglaRepository.saveAll(List.of(
                    regla("MAX_KG_FORMATO", "RN-02: máximo de kg por producto/formato en el carrito", "12.5", "kg", hoy),
                    regla("TEMP_MAX_ALERTA", "RN-01: sobre esta temperatura se genera alerta de frío", "-15", "°C", hoy),
                    regla("MIN_SOSTENIDO_CRITICO", "RN-01: minutos sobre el umbral para alerta crítica y bloqueo de lote", "30", "min", hoy),
                    regla("STOCK_CRITICO", "Bajo estos kg disponibles el producto queda en stock crítico", "35", "kg", hoy),
                    regla("RUTA_MAX_MIN", "Minutos máximos de ruta antes de perder la cadena de frío", "90", "min", hoy),
                    regla("AVISO_RUTA_MIN", "Minutos restantes en que se avisa al repartidor", "15", "min", hoy),
                    regla("RESERVA_MIN", "Minutos que dura la reserva de stock de un pedido sin pagar", "30", "min", hoy),
                    regla("DIAS_MAX_ENTREGA", "Días máximos hacia adelante para programar la entrega", "14", "días", hoy),
                    regla("VIP_MONTO", "Gasto acumulado para segmento VIP", "500000", "CLP", hoy),
                    regla("FRECUENTE_PEDIDOS", "Pedidos entregados para segmento Frecuente", "3", "pedidos", hoy)));
        }
        log.info("ms-config: datos de ejemplo verificados");
    }

    private ReglaNegocio regla(String codigo, String descripcion, String valor, String unidad, LocalDate hoy) {
        return new ReglaNegocio(codigo, descripcion, new BigDecimal(valor), unidad, hoy);
    }
}
