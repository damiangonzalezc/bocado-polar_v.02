package cl.bocadopolar.pedidos.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import cl.bocadopolar.pedidos.model.EstadoPedido;
import cl.bocadopolar.pedidos.model.HistorialEstado;
import cl.bocadopolar.pedidos.model.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class PedidoDtos {

    public static final List<String> HORARIOS = List.of("09:00 - 12:00", "12:00 - 15:00", "15:00 - 18:00");

    private PedidoDtos() {
    }

    /* ---------- Carrito ---------- */

    /** Para obtener (o crear) el carrito: con clienteId si hay sesión, o con sesionAnonimaId si es visitante. */
    public record CarritoRequest(Long clienteId, @Size(max = 36) String sesionAnonimaId) {
    }

    public record ItemCarritoRequest(@NotNull Long productoId,
            @NotNull @DecimalMin(value = "0.5", message = "Mínimo 0,5 kg") BigDecimal kg) {
    }

    /** kg = 0 quita el producto. */
    public record CantidadRequest(@NotNull @DecimalMin("0") BigDecimal kg) {
    }

    public record AsociarClienteRequest(@NotNull Long clienteId) {
    }

    /* ---------- Checkout ---------- */

    public record CheckoutRequest(
            @NotNull Long carritoId,
            @NotNull Long clienteId,
            @NotBlank @Size(max = 80) String nombreCliente,
            @NotBlank @Email @Size(max = 120) String email,
            @Pattern(regexp = "^$|^(\\+?56)?\\s?9\\s?\\d{4}\\s?\\d{4}$", message = "Formato: +56 9 1234 5678") String telefono,
            @NotBlank @Size(min = 5, max = 100) String direccion,
            @NotBlank @Size(max = 60) String comuna,
            @Size(max = 120) String referencia,
            @Size(max = 300) String comentario,
            @NotNull LocalDate fechaEntrega,
            @NotBlank String horario,
            @NotNull MetodoPago metodoPago,
            @Pattern(regexp = "^$|^[A-Za-z0-9]{4,16}$", message = "4 a 16 letras o números") String codigoCupon) {
    }

    /** Vista previa de totales antes de confirmar (no guarda nada). */
    public record CotizacionRequest(@NotNull Long carritoId, @NotBlank String comuna, Long clienteId, String codigoCupon) {
    }

    public record CotizacionResponse(BigDecimal subtotal, BigDecimal envio, BigDecimal descuento, BigDecimal total,
            BigDecimal totalKg, String mensajeCupon) {
    }

    /* ---------- Estados ---------- */

    public record CambioEstadoRequest(@NotNull EstadoPedido estado, @Size(max = 300) String nota, Long usuarioId) {
    }

    /** El admin asigna el pedido a un repartidor (lo llama ms-rutas al crear la parada). */
    public record AsignacionRepartidorRequest(@NotNull Long repartidorId, @Size(max = 80) String repartidorNombre,
            Long usuarioId) {
    }

    public record MotivoRequest(@NotBlank @Size(max = 300) String motivo, Long usuarioId) {
    }

    /** Lo envía ms-pagos. evento: APROBADO | RECHAZADO | CONTRA_ENTREGA | REEMBOLSADO */
    public record PagoEventoRequest(@NotBlank String evento, Long pagoId, String detalle) {
    }

    public record NotaRequest(@NotBlank @Size(max = 500) String texto, Long autorId) {
    }

    public record PasoSeguimiento(EstadoPedido estado, boolean completado, Instant fecha) {
    }

    public record SeguimientoResponse(Long pedidoId, String codigo, EstadoPedido estado, LocalDate fechaEntrega,
            String horario, List<PasoSeguimiento> pasos, List<HistorialEstado> historial) {
    }

    public record EtiquetaResponse(String codigo, String cliente, String telefono, String direccion, String comuna,
            String referencia, LocalDate fechaEntrega, String horario, BigDecimal totalKg, int productos,
            boolean cobrarAlEntregar, BigDecimal montoACobrar, String indicacion) {
    }
}
