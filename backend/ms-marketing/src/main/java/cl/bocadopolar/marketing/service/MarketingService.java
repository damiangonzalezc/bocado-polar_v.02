package cl.bocadopolar.marketing.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bocadopolar.marketing.client.Clientes.CarritoDto;
import cl.bocadopolar.marketing.client.Clientes.ClienteDto;
import cl.bocadopolar.marketing.client.Clientes.ClientesClient;
import cl.bocadopolar.marketing.client.Clientes.NotificacionesClient;
import cl.bocadopolar.marketing.client.Clientes.PedidosClient;
import cl.bocadopolar.marketing.dto.MarketingDtos.AbandonadoRequest;
import cl.bocadopolar.marketing.dto.MarketingDtos.CanalRequest;
import cl.bocadopolar.marketing.dto.MarketingDtos.CuponRequest;
import cl.bocadopolar.marketing.dto.MarketingDtos.ValidacionCupon;
import cl.bocadopolar.marketing.exception.RecursoNoEncontradoException;
import cl.bocadopolar.marketing.exception.ReglaNegocioException;
import cl.bocadopolar.marketing.exception.SolicitudInvalidaException;
import cl.bocadopolar.marketing.model.CanalCampana;
import cl.bocadopolar.marketing.model.CarritoAbandonado;
import cl.bocadopolar.marketing.model.Cupon;
import cl.bocadopolar.marketing.model.TipoDescuento;
import cl.bocadopolar.marketing.model.UsoCupon;
import cl.bocadopolar.marketing.repository.CanalCampanaRepository;
import cl.bocadopolar.marketing.repository.CarritoAbandonadoRepository;
import cl.bocadopolar.marketing.repository.CuponRepository;
import cl.bocadopolar.marketing.repository.UsoCuponRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketingService {

    private final CuponRepository cuponRepository;
    private final UsoCuponRepository usoRepository;
    private final CanalCampanaRepository canalRepository;
    private final CarritoAbandonadoRepository abandonadoRepository;
    private final PedidosClient pedidos;
    private final ClientesClient clientes;
    private final NotificacionesClient notificaciones;

    /* =============== Cupones =============== */

    public List<Cupon> listarCupones() {
        return cuponRepository.findAll();
    }

    public Cupon obtenerCupon(Long id) {
        return cuponRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el cupón " + id));
    }

    public Cupon porCodigo(String codigo) {
        return cuponRepository.findByCodigoIgnoreCase(codigo.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el cupón " + codigo));
    }

    @Transactional
    public Cupon crearCupon(CuponRequest req) {
        if (cuponRepository.findByCodigoIgnoreCase(req.codigo()).isPresent()) {
            throw new ReglaNegocioException("Ya existe el cupón " + req.codigo().toUpperCase());
        }
        Cupon c = new Cupon();
        copiar(req, c);
        return cuponRepository.save(c);
    }

    @Transactional
    public Cupon actualizarCupon(Long id, CuponRequest req) {
        Cupon c = obtenerCupon(id);
        cuponRepository.findByCodigoIgnoreCase(req.codigo())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ReglaNegocioException("Ya existe el cupón " + req.codigo().toUpperCase());
                });
        copiar(req, c);
        return cuponRepository.save(c);
    }

    @Transactional
    public Cupon cambiarActivo(Long id, boolean activo) {
        Cupon c = obtenerCupon(id);
        c.setActivo(activo);
        return cuponRepository.save(c);
    }

    @Transactional
    public void eliminarCupon(Long id) {
        Cupon c = obtenerCupon(id);
        if (c.getUsos() > 0) {
            throw new ReglaNegocioException("El cupón ya se usó " + c.getUsos() + " veces; desactívalo en vez de eliminarlo");
        }
        cuponRepository.delete(c);
    }

    /** CU-07.03 Validar cupón en el checkout. Siempre responde 200 con valido=true/false y el motivo. */
    public ValidacionCupon validar(String codigo, Long clienteId, BigDecimal subtotal) {
        String cod = codigo.trim().toUpperCase();
        Cupon c = cuponRepository.findByCodigoIgnoreCase(cod).orElse(null);
        if (c == null) {
            return new ValidacionCupon(false, cod, BigDecimal.ZERO, "El cupón " + cod + " no existe");
        }
        if (!Boolean.TRUE.equals(c.getActivo())) {
            return new ValidacionCupon(false, cod, BigDecimal.ZERO, "El cupón no está activo");
        }
        if (c.getVence().isBefore(java.time.LocalDate.now())) {
            return new ValidacionCupon(false, cod, BigDecimal.ZERO, "El cupón venció el " + c.getVence());
        }
        if (c.getUsos() >= c.getLimiteUsos()) {
            return new ValidacionCupon(false, cod, BigDecimal.ZERO, "El cupón alcanzó su límite de usos");
        }
        if (subtotal.compareTo(c.getCompraMinima()) < 0) {
            return new ValidacionCupon(false, cod, BigDecimal.ZERO, "La compra mínima para este cupón es $" + c.getCompraMinima());
        }
        if (clienteId != null && usoRepository.countByCuponIdAndClienteId(c.getId(), clienteId) >= c.getLimitePorCliente()) {
            return new ValidacionCupon(false, cod, BigDecimal.ZERO, "Ya usaste este cupón");
        }
        BigDecimal descuento = c.calcularDescuento(subtotal);
        String texto = c.getTipo() == TipoDescuento.PORCENTAJE ? c.getValor() + "% de descuento" : "$" + c.getValor() + " de descuento";
        return new ValidacionCupon(true, cod, descuento, "Cupón aplicado: " + texto);
    }

    /** Lo llama ms-pedidos cuando el pedido se crea con cupón. */
    @Transactional
    public UsoCupon registrarUso(String codigo, Long pedidoId, Long clienteId) {
        Cupon c = porCodigo(codigo);
        if (usoRepository.existsByPedidoId(pedidoId)) {
            throw new ReglaNegocioException("El pedido " + pedidoId + " ya registró un cupón");
        }
        if (c.getUsos() >= c.getLimiteUsos()) {
            throw new ReglaNegocioException("El cupón alcanzó su límite de usos");
        }
        c.setUsos(c.getUsos() + 1);
        cuponRepository.save(c);
        return usoRepository.save(new UsoCupon(null, c.getId(), pedidoId, clienteId, Instant.now()));
    }

    public List<UsoCupon> usos(Long cuponId) {
        return usoRepository.findByCuponIdOrderByFechaDesc(cuponId);
    }

    /* =============== Canales =============== */

    public List<CanalCampana> canales() {
        return canalRepository.findAll();
    }

    @Transactional
    public CanalCampana crearCanal(CanalRequest req) {
        if (canalRepository.findByCanalIgnoreCase(req.canal().trim()).isPresent()) {
            throw new ReglaNegocioException("Ya existe el canal " + req.canal());
        }
        return canalRepository.save(new CanalCampana(null, req.canal().trim(), cero(req.visitas()), cero(req.pedidos()),
                req.ingresos() == null ? BigDecimal.ZERO : req.ingresos()));
    }

    @Transactional
    public CanalCampana actualizarCanal(Long id, CanalRequest req) {
        CanalCampana c = canalRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el canal " + id));
        c.setCanal(req.canal().trim());
        c.setVisitas(cero(req.visitas()));
        c.setPedidos(cero(req.pedidos()));
        c.setIngresos(req.ingresos() == null ? BigDecimal.ZERO : req.ingresos());
        return canalRepository.save(c);
    }

    @Transactional
    public void eliminarCanal(Long id) {
        canalRepository.deleteById(id);
    }

    /** Suma una visita al canal (si no existe, lo crea). Lo llama ms-reportes. */
    @Transactional
    public CanalCampana registrarVisita(String canal) {
        CanalCampana c = canalRepository.findByCanalIgnoreCase(canal.trim())
                .orElseGet(() -> new CanalCampana(null, canal.trim(), 0, 0, BigDecimal.ZERO));
        c.setVisitas(c.getVisitas() + 1);
        return canalRepository.save(c);
    }

    @Transactional
    public CanalCampana registrarConversion(String canal, BigDecimal monto) {
        CanalCampana c = canalRepository.findByCanalIgnoreCase(canal.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el canal " + canal));
        c.setPedidos(c.getPedidos() + 1);
        c.setIngresos(c.getIngresos().add(monto));
        return canalRepository.save(c);
    }

    /* =============== Carritos abandonados =============== */

    public List<CarritoAbandonado> abandonados(Boolean recuperado) {
        return abandonadoRepository.findAllByOrderByFechaDesc().stream()
                .filter(a -> recuperado == null || recuperado.equals(a.getRecuperado()))
                .toList();
    }

    @Transactional
    public CarritoAbandonado registrarAbandonado(AbandonadoRequest req) {
        CarritoAbandonado a = new CarritoAbandonado();
        a.setCliente(req.cliente() == null || req.cliente().isBlank() ? "Visitante" : req.cliente());
        a.setEmail(req.email());
        a.setResumen(req.resumen());
        a.setTotal(req.total());
        a.setFecha(Instant.now());
        return abandonadoRepository.save(a);
    }

    /** CU-07.05 Revisa en ms-pedidos los carritos sin movimiento y registra los nuevos. */
    @Transactional
    public List<CarritoAbandonado> detectar(int horas) {
        List<CarritoAbandonado> nuevos = new ArrayList<>();
        for (CarritoDto carrito : pedidos.carritosInactivos(horas)) {
            if (abandonadoRepository.existsByCarritoIdAndRecuperadoFalse(carrito.id())) {
                continue;
            }
            CarritoAbandonado a = new CarritoAbandonado();
            a.setCarritoId(carrito.id());
            a.setClienteId(carrito.clienteId());
            a.setCliente("Visitante");
            if (carrito.clienteId() != null) {
                ClienteDto c = clientes.cliente(carrito.clienteId());
                if (c != null) {
                    a.setCliente(c.apellido() == null ? c.nombre() : c.nombre() + " " + c.apellido());
                    a.setEmail(c.email());
                }
            }
            String resumen = carrito.items() == null ? "" : carrito.items().stream()
                    .map(i -> i.nombre() + " · " + i.kg().stripTrailingZeros().toPlainString().replace('.', ',') + " kg")
                    .collect(Collectors.joining(", "));
            a.setResumen(resumen.length() > 300 ? resumen.substring(0, 297) + "..." : resumen);
            a.setTotal(carrito.subtotal() == null ? BigDecimal.ZERO : carrito.subtotal());
            a.setFecha(Instant.now());
            nuevos.add(abandonadoRepository.save(a));
        }
        return nuevos;
    }

    /** CU-07.06 Envía el correo de rescate (opcionalmente con un cupón). */
    @Transactional
    public CarritoAbandonado enviarRescate(Long id, String codigoCupon) {
        CarritoAbandonado a = abandonadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el carrito abandonado " + id));
        if (a.getEmail() == null || a.getEmail().isBlank()) {
            throw new ReglaNegocioException("El carrito es de un visitante sin correo: no se puede rescatar");
        }
        if (Boolean.TRUE.equals(a.getRecuperado())) {
            throw new ReglaNegocioException("Este carrito ya se recuperó");
        }
        String cupon = codigoCupon == null || codigoCupon.isBlank() ? null : codigoCupon.trim().toUpperCase();
        if (cupon != null && !porCodigo(cupon).estaVigente()) {
            throw new SolicitudInvalidaException("El cupón " + cupon + " no está vigente");
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("cliente", a.getCliente());
        vars.put("resumen", a.getResumen());
        vars.put("total", a.getTotal());
        vars.put("cupon", cupon == null ? "" : cupon);
        notificaciones.enviar("RESCATE_CARRITO", "CORREO", a.getEmail(), vars);
        a.setCorreoEnviado(true);
        a.setCorreoEnviadoEn(Instant.now());
        a.setCuponOfrecido(cupon);
        return abandonadoRepository.save(a);
    }

    @Transactional
    public CarritoAbandonado marcarRecuperado(Long id, Long pedidoId) {
        CarritoAbandonado a = abandonadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el carrito abandonado " + id));
        a.setRecuperado(true);
        a.setPedidoRecuperadoId(pedidoId);
        return abandonadoRepository.save(a);
    }

    /* =============== utilidades =============== */

    private void copiar(CuponRequest req, Cupon c) {
        if (req.tipo() == TipoDescuento.PORCENTAJE && req.valor().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new SolicitudInvalidaException("Un descuento porcentual no puede superar el 100%");
        }
        c.setCodigo(req.codigo().trim().toUpperCase());
        c.setTipo(req.tipo());
        c.setValor(req.valor());
        c.setCompraMinima(req.compraMinima() == null ? BigDecimal.ZERO : req.compraMinima());
        c.setLimiteUsos(req.limiteUsos());
        c.setLimitePorCliente(req.limitePorCliente() == null ? 1 : req.limitePorCliente());
        if (req.activo() != null) {
            c.setActivo(req.activo());
        }
        c.setVence(req.vence());
    }

    private static int cero(Integer v) {
        return v == null ? 0 : v;
    }
}
