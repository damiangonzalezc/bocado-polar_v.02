package cl.bocadopolar.pagos.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cl.bocadopolar.pagos.model.Boleta;
import cl.bocadopolar.pagos.model.CobroContraEntrega;
import cl.bocadopolar.pagos.model.EstadoPago;
import cl.bocadopolar.pagos.model.MetodoPago;
import cl.bocadopolar.pagos.model.Pago;
import cl.bocadopolar.pagos.model.TipoCobro;
import cl.bocadopolar.pagos.repository.BoletaRepository;
import cl.bocadopolar.pagos.repository.CobroContraEntregaRepository;
import cl.bocadopolar.pagos.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Pagos de los pedidos "de hoy" de ms-pedidos (IDs 1 a 7; el 5 sigue pendiente de pago). */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bocado.semilla.habilitada", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {

    private final PagoRepository pagoRepository;
    private final CobroContraEntregaRepository cobroRepository;
    private final BoletaRepository boletaRepository;

    @Override
    public void run(String... args) {
        if (pagoRepository.count() > 0) {
            return;
        }
        Instant hace = Instant.now().minus(3, ChronoUnit.HOURS);
        pago(1L, "BP-100001", MetodoPago.WEBPAY, EstadoPago.APROBADO, 48455, "Webpay", hace);
        Pago cod = pago(2L, "BP-100002", MetodoPago.CONTRA_ENTREGA, EstadoPago.PENDIENTE, 183370, "Contra entrega", hace);
        cobro(cod, TipoCobro.EFECTIVO, "200000", false, null);
        pago(3L, "BP-100003", MetodoPago.WEBPAY, EstadoPago.APROBADO, 26470, "Webpay", hace);
        pago(4L, "BP-100004", MetodoPago.TRANSFERENCIA, EstadoPago.APROBADO, 52930, "Transferencia bancaria", hace);
        Pago entregado = pago(6L, "BP-100006", MetodoPago.CONTRA_ENTREGA, EstadoPago.APROBADO, 30470, "Contra entrega", hace);
        cobro(entregado, TipoCobro.POS_MOVIL, null, true, 6L);
        pago(7L, "BP-100007", MetodoPago.WEBPAY, EstadoPago.APROBADO, 25460, "Webpay", hace);
        log.info("ms-pagos: pagos de ejemplo creados");
    }

    private Pago pago(Long pedidoId, String codigo, MetodoPago metodo, EstadoPago estado, int monto, String pasarela,
            Instant fecha) {
        Pago p = new Pago();
        p.setPedidoId(pedidoId);
        p.setCodigoPedido(codigo);
        p.setMetodo(metodo);
        p.setEstado(estado);
        p.setMonto(BigDecimal.valueOf(monto));
        p.setPasarela(pasarela);
        p.setReferencia(estado == EstadoPago.APROBADO ? "AUT-DEMO-" + pedidoId : null);
        p.setIdempotencyKey("semilla-" + pedidoId);
        p.setFecha(fecha);
        p = pagoRepository.save(p);
        if (estado == EstadoPago.APROBADO) {
            BigDecimal total = p.getMonto();
            BigDecimal neto = total.divide(new BigDecimal("1.19"), 0, RoundingMode.HALF_UP);
            Boleta b = boletaRepository.save(new Boleta(null, "TMP" + p.getId(), p.getId(), pedidoId, neto,
                    total.subtract(neto), new BigDecimal("19"), total, fecha));
            b.setNumero(String.format("B-%06d", b.getId()));
            boletaRepository.save(b);
        }
        return p;
    }

    private void cobro(Pago p, TipoCobro tipo, String pagaCon, boolean cobrado, Long repartidorId) {
        CobroContraEntrega c = new CobroContraEntrega();
        c.setPagoId(p.getId());
        c.setPedidoId(p.getPedidoId());
        c.setTipo(tipo);
        c.setMonto(p.getMonto());
        c.setPagaCon(pagaCon == null ? null : new BigDecimal(pagaCon));
        c.setVuelto(c.calcularVuelto());
        c.setCobrado(cobrado);
        c.setCobradoPorId(repartidorId);
        c.setFechaCobro(cobrado ? Instant.now().minus(1, ChronoUnit.HOURS) : null);
        cobroRepository.save(c);
    }
}
