package cl.bocadopolar.pagos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bocadopolar.pagos.client.Clientes.ConfigClient;
import cl.bocadopolar.pagos.client.Clientes.ItemDto;
import cl.bocadopolar.pagos.client.Clientes.PedidoDto;
import cl.bocadopolar.pagos.client.Clientes.PedidosClient;
import cl.bocadopolar.pagos.dto.PagoDtos.CobroEntregaRequest;
import cl.bocadopolar.pagos.dto.PagoDtos.IniciarPagoRequest;
import cl.bocadopolar.pagos.dto.PagoDtos.ReembolsoRequest;
import cl.bocadopolar.pagos.dto.PagoDtos.WebhookRequest;
import cl.bocadopolar.pagos.exception.RecursoNoEncontradoException;
import cl.bocadopolar.pagos.exception.ReglaNegocioException;
import cl.bocadopolar.pagos.exception.SolicitudInvalidaException;
import cl.bocadopolar.pagos.model.Boleta;
import cl.bocadopolar.pagos.model.CobroContraEntrega;
import cl.bocadopolar.pagos.model.EstadoPago;
import cl.bocadopolar.pagos.model.EstadoReembolso;
import cl.bocadopolar.pagos.model.MetodoPago;
import cl.bocadopolar.pagos.model.Pago;
import cl.bocadopolar.pagos.model.Reembolso;
import cl.bocadopolar.pagos.model.TipoCobro;
import cl.bocadopolar.pagos.repository.BoletaRepository;
import cl.bocadopolar.pagos.repository.CobroContraEntregaRepository;
import cl.bocadopolar.pagos.repository.PagoRepository;
import cl.bocadopolar.pagos.repository.ReembolsoRepository;
import lombok.RequiredArgsConstructor;

/**
 * Pagos simulados. Flujo:
 * - WEBPAY / TARJETA: el pago queda PENDIENTE y se confirma con POST /api/v1/pagos/webhook (simula a Webpay).
 * - TRANSFERENCIA: queda PENDIENTE hasta que el personal la confirma.
 * - CONTRA_ENTREGA: queda PENDIENTE, el pedido pasa a EN_PREPARACION y el repartidor cobra al entregar.
 */
@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final CobroContraEntregaRepository cobroRepository;
    private final BoletaRepository boletaRepository;
    private final ReembolsoRepository reembolsoRepository;
    private final PedidosClient pedidos;
    private final ConfigClient config;

    /* =============== Pagos =============== */

    @Transactional
    public Pago iniciar(IniciarPagoRequest req) {
        if (req.idempotencyKey() != null && !req.idempotencyKey().isBlank()) {
            var previo = pagoRepository.findByIdempotencyKey(req.idempotencyKey());
            if (previo.isPresent()) {
                return previo.get();
            }
        }
        PedidoDto pedido = pedidos.pedido(req.pedidoId());
        if (!"PENDIENTE_PAGO".equals(pedido.estado())) {
            throw new ReglaNegocioException("El pedido " + pedido.codigo() + " está " + pedido.estado() + " y no admite pagos");
        }
        List<Pago> anteriores = pagoRepository.findByPedidoIdOrderByFechaDesc(req.pedidoId());
        if (anteriores.stream().anyMatch(p -> p.getEstado() == EstadoPago.APROBADO)) {
            throw new ReglaNegocioException("El pedido ya tiene un pago aprobado");
        }
        var pendiente = anteriores.stream()
                .filter(p -> p.getEstado() == EstadoPago.PENDIENTE && p.getMetodo() == req.metodo()).findFirst();
        if (pendiente.isPresent()) {
            return pendiente.get();
        }

        String pasarela = switch (req.metodo()) {
            case WEBPAY, TARJETA -> "Webpay";
            case TRANSFERENCIA -> "Transferencia bancaria";
            case CONTRA_ENTREGA -> "Contra entrega";
        };
        if (req.metodo() != MetodoPago.CONTRA_ENTREGA && !config.pasarelaDisponible(pasarela)) {
            throw new ReglaNegocioException("La pasarela " + pasarela + " no está disponible en este momento");
        }

        Pago pago = new Pago();
        pago.setPedidoId(pedido.id());
        pago.setCodigoPedido(pedido.codigo());
        pago.setMetodo(req.metodo());
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setMonto(pedido.total());
        pago.setPasarela(pasarela);
        pago.setIdempotencyKey(req.idempotencyKey() == null || req.idempotencyKey().isBlank()
                ? UUID.randomUUID().toString() : req.idempotencyKey());
        pago.setFecha(Instant.now());
        pago.setInstrucciones(switch (req.metodo()) {
            case WEBPAY, TARJETA -> "Confirma el pago en Webpay (simulado: POST /api/v1/pagos/webhook)";
            case TRANSFERENCIA -> "Transfiere $" + pedido.total() + " a BOCADO Polar SpA, Cta. Cte. 12345678, "
                    + "Banco Estado, indicando el código " + pedido.codigo();
            case CONTRA_ENTREGA -> "Pagas al recibir: efectivo, POS móvil o transferencia";
        });
        pago = pagoRepository.save(pago);

        if (req.metodo() == MetodoPago.CONTRA_ENTREGA) {
            CobroContraEntrega cobro = new CobroContraEntrega();
            cobro.setPagoId(pago.getId());
            cobro.setPedidoId(pedido.id());
            cobro.setTipo(req.tipoCobro() == null ? TipoCobro.EFECTIVO : req.tipoCobro());
            cobro.setMonto(pedido.total());
            cobro.setPagaCon(req.pagaCon());
            if (cobro.getTipo() == TipoCobro.EFECTIVO && req.pagaCon() != null
                    && req.pagaCon().compareTo(pedido.total()) < 0) {
                throw new SolicitudInvalidaException("El monto con que paga es menor al total del pedido");
            }
            cobro.setVuelto(cobro.calcularVuelto());
            cobro.setCobrado(false);
            cobroRepository.save(cobro);
            pedidos.notificarPago(pedido.id(), "CONTRA_ENTREGA", pago.getId(), cobro.getTipo().name());
        }
        return pago;
    }

    public List<Pago> listar(EstadoPago estado, MetodoPago metodo) {
        return pagoRepository.findAll().stream()
                .filter(p -> estado == null || p.getEstado() == estado)
                .filter(p -> metodo == null || p.getMetodo() == metodo)
                .sorted(Comparator.comparing(Pago::getFecha).reversed())
                .toList();
    }

    public Pago obtener(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pago " + id));
    }

    public List<Pago> delPedido(Long pedidoId) {
        return pagoRepository.findByPedidoIdOrderByFechaDesc(pedidoId);
    }

    /** CU-06.08 Confirmación de la pasarela (simulada). */
    @Transactional
    public Pago webhook(WebhookRequest req) {
        Pago pago = obtener(req.pagoId());
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            return pago;
        }
        if (pago.getMetodo() != MetodoPago.WEBPAY && pago.getMetodo() != MetodoPago.TARJETA) {
            throw new ReglaNegocioException("El webhook solo aplica a pagos con Webpay o tarjeta");
        }
        if (Boolean.TRUE.equals(req.aprobado())) {
            pago.setReferencia(req.codigoAutorizacion() == null ? "AUT-" + pago.getId() : req.codigoAutorizacion());
            aprobar(pago, true);
        } else {
            pago.setEstado(EstadoPago.RECHAZADO);
            pago.setMotivoRechazo(req.motivo() == null ? "Rechazado por la pasarela" : req.motivo());
            pago.setActualizadoEn(Instant.now());
            pagoRepository.save(pago);
            avisarRechazo(pago);
        }
        return pago;
    }

    @Transactional
    public Pago confirmarTransferencia(Long id) {
        Pago pago = obtener(id);
        if (pago.getMetodo() != MetodoPago.TRANSFERENCIA) {
            throw new ReglaNegocioException("El pago " + id + " no es por transferencia");
        }
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            throw new ReglaNegocioException("El pago ya está " + pago.getEstado());
        }
        pago.setReferencia("TRF-" + pago.getId());
        aprobar(pago, true);
        return pago;
    }

    /** CU-06.05 El repartidor confirma el cobro al entregar. */
    @Transactional
    public CobroContraEntrega confirmarCobroEntrega(Long pagoId, CobroEntregaRequest req) {
        Pago pago = obtener(pagoId);
        CobroContraEntrega cobro = cobroRepository.findByPagoId(pagoId)
                .orElseThrow(() -> new ReglaNegocioException("El pago " + pagoId + " no es contra entrega"));
        if (Boolean.TRUE.equals(cobro.getCobrado())) {
            return cobro;
        }
        cobro.setTipo(req.tipo());
        if (req.pagaCon() != null) {
            cobro.setPagaCon(req.pagaCon());
        }
        if (req.tipo() == TipoCobro.EFECTIVO && cobro.getPagaCon() != null
                && cobro.getPagaCon().compareTo(cobro.getMonto()) < 0) {
            throw new SolicitudInvalidaException("El efectivo recibido es menor al monto a cobrar");
        }
        cobro.setVuelto(cobro.calcularVuelto());
        cobro.setCobrado(true);
        cobro.setCobradoPorId(req.repartidorId());
        cobro.setFechaCobro(Instant.now());
        cobroRepository.save(cobro);
        pago.setReferencia("CE-" + req.tipo().name());
        aprobar(pago, false);
        return cobro;
    }

    public CobroContraEntrega cobroDe(Long pagoId) {
        return cobroRepository.findByPagoId(pagoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El pago " + pagoId + " no tiene cobro contra entrega"));
    }

    /** Cobros hechos por un repartidor en un día (para el cierre de caja). */
    public List<CobroContraEntrega> cobrosDe(Long repartidorId, LocalDate fecha) {
        ZoneId zona = ZoneId.systemDefault();
        return cobroRepository.findByCobradoPorIdAndCobradoTrue(repartidorId).stream()
                .filter(c -> fecha == null || (c.getFechaCobro() != null
                        && c.getFechaCobro().atZone(zona).toLocalDate().equals(fecha)))
                .toList();
    }

    /* =============== Boletas =============== */

    public Boleta boleta(Long id) {
        return boletaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la boleta " + id));
    }

    public Boleta boletaDePedido(Long pedidoId) {
        return boletaRepository.findByPedidoIdOrderByEmisionDesc(pedidoId).stream().findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("El pedido " + pedidoId + " aún no tiene boleta"));
    }

    /** CU-06.04 Boleta en HTML lista para imprimir (o guardar como PDF desde el navegador). */
    public String boletaHtml(Long id) {
        Boleta b = boleta(id);
        PedidoDto p = pedidos.pedido(b.getPedidoId());
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(ZoneId.systemDefault());
        StringBuilder filas = new StringBuilder();
        if (p.items() != null) {
            for (ItemDto i : p.items()) {
                filas.append("<tr><td>").append(esc(i.nombre())).append("</td><td>").append(i.kg())
                        .append(" kg</td><td>$").append(i.precioKg()).append("</td><td>$")
                        .append(i.subtotal()).append("</td></tr>");
            }
        }
        return """
                <!doctype html><html lang="es"><head><meta charset="utf-8"><title>Boleta %s</title>
                <style>body{font-family:Arial,sans-serif;max-width:640px;margin:24px auto;color:#111}
                table{width:100%%;border-collapse:collapse}td,th{padding:6px;border-bottom:1px solid #ddd;text-align:left}
                .tot td{font-weight:bold}h1{margin:0}small{color:#555}</style></head><body>
                <h1>BOCADO Polar SpA</h1><small>Boleta electrónica N° %s · %s</small>
                <p><b>Pedido:</b> %s<br><b>Cliente:</b> %s<br><b>Dirección:</b> %s, %s</p>
                <table><tr><th>Producto</th><th>Cantidad</th><th>Precio/kg</th><th>Subtotal</th></tr>%s
                <tr><td colspan="3">Envío</td><td>$%s</td></tr>
                <tr><td colspan="3">Descuento</td><td>-$%s</td></tr>
                <tr><td colspan="3">Neto</td><td>$%s</td></tr>
                <tr><td colspan="3">IVA (%s%%)</td><td>$%s</td></tr>
                <tr class="tot"><td colspan="3">Total</td><td>$%s</td></tr></table>
                <p><small>Documento simulado para fines académicos.</small></p></body></html>
                """.formatted(b.getNumero(), b.getNumero(), f.format(b.getEmision()), esc(p.codigo()),
                esc(p.nombreCliente()), esc(p.direccion()), esc(p.comuna()), filas, p.envio(), p.descuento(),
                b.getNeto(), b.getTasaIva().stripTrailingZeros().toPlainString(), b.getIva(), b.getTotal());
    }

    /* =============== Reembolsos =============== */

    @Transactional
    public Reembolso solicitarReembolso(Long pagoId, ReembolsoRequest req) {
        Pago pago = obtener(pagoId);
        if (pago.getEstado() != EstadoPago.APROBADO) {
            throw new ReglaNegocioException("Solo se reembolsan pagos aprobados (este está " + pago.getEstado() + ")");
        }
        if (reembolsoRepository.existsByPagoIdAndEstado(pagoId, EstadoReembolso.SOLICITADO)) {
            throw new ReglaNegocioException("Ya hay un reembolso pendiente para este pago");
        }
        BigDecimal monto = req.monto() == null ? pago.getMonto() : req.monto();
        if (monto.compareTo(pago.getMonto()) > 0) {
            throw new SolicitudInvalidaException("El reembolso no puede superar el monto pagado");
        }
        return reembolsoRepository.save(new Reembolso(null, pagoId, pago.getPedidoId(), req.motivo(), monto,
                EstadoReembolso.SOLICITADO, req.solicitadoPorId(), null, null, Instant.now()));
    }

    public List<Reembolso> reembolsos(EstadoReembolso estado) {
        return estado == null ? reembolsoRepository.findAllByOrderByFechaDesc()
                : reembolsoRepository.findByEstadoOrderByFechaDesc(estado);
    }

    @Transactional
    public Reembolso aprobarReembolso(Long id, Long usuarioId) {
        Reembolso r = reembolso(id);
        if (r.getEstado() != EstadoReembolso.SOLICITADO) {
            throw new ReglaNegocioException("El reembolso ya está " + r.getEstado());
        }
        r.setEstado(EstadoReembolso.APROBADO);
        r.setAprobadoPorId(usuarioId);
        reembolsoRepository.save(r);
        Pago pago = obtener(r.getPagoId());
        pago.setEstado(EstadoPago.REEMBOLSADO);
        pago.setActualizadoEn(Instant.now());
        pagoRepository.save(pago);
        pedidos.notificarPago(pago.getPedidoId(), "REEMBOLSADO", pago.getId(), "Reembolso $" + r.getMonto());
        return r;
    }

    @Transactional
    public Reembolso rechazarReembolso(Long id, String motivo, Long usuarioId) {
        Reembolso r = reembolso(id);
        if (r.getEstado() != EstadoReembolso.SOLICITADO) {
            throw new ReglaNegocioException("El reembolso ya está " + r.getEstado());
        }
        r.setEstado(EstadoReembolso.RECHAZADO);
        r.setMotivoRechazo(motivo);
        r.setAprobadoPorId(usuarioId);
        return reembolsoRepository.save(r);
    }

    private Reembolso reembolso(Long id) {
        return reembolsoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el reembolso " + id));
    }

    /* =============== internos =============== */

    private void aprobar(Pago pago, boolean avisarPedido) {
        pago.setEstado(EstadoPago.APROBADO);
        pago.setActualizadoEn(Instant.now());
        pagoRepository.save(pago);
        emitirBoleta(pago);
        if (avisarPedido) {
            pedidos.notificarPago(pago.getPedidoId(), "APROBADO", pago.getId(), pago.getMetodo().name());
        }
    }

    /** CU-06.04 Boleta: el total ya trae IVA; neto = total / (1 + IVA). */
    Boleta emitirBoleta(Pago pago) {
        var existente = boletaRepository.findByPagoId(pago.getId());
        if (existente.isPresent()) {
            return existente.get();
        }
        BigDecimal tasa = config.iva();
        BigDecimal neto = pago.getMonto().divide(BigDecimal.ONE.add(tasa.divide(BigDecimal.valueOf(100))), 0,
                RoundingMode.HALF_UP);
        BigDecimal iva = pago.getMonto().subtract(neto);
        Boleta b = boletaRepository.save(new Boleta(null, "TMP" + pago.getId(), pago.getId(), pago.getPedidoId(),
                neto, iva, tasa, pago.getMonto(), Instant.now()));
        b.setNumero(String.format("B-%06d", b.getId()));
        return boletaRepository.save(b);
    }

    private void avisarRechazo(Pago pago) {
        try {
            pedidos.notificarPago(pago.getPedidoId(), "RECHAZADO", pago.getId(), pago.getMotivoRechazo());
        } catch (RuntimeException e) {
            // el rechazo solo queda informativo en el pedido; no se deshace el registro del pago
        }
    }

    private static String esc(String t) {
        if (t == null) {
            return "";
        }
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
