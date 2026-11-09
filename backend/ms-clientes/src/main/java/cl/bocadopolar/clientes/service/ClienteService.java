package cl.bocadopolar.clientes.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bocadopolar.clientes.client.ConfigClient;
import cl.bocadopolar.clientes.dto.ClienteDtos.ClienteRequest;
import cl.bocadopolar.clientes.dto.ClienteDtos.DireccionRequest;
import cl.bocadopolar.clientes.dto.ClienteDtos.MedioPagoRequest;
import cl.bocadopolar.clientes.dto.ClienteDtos.NotaRequest;
import cl.bocadopolar.clientes.dto.ClienteDtos.PerfilRequest;
import cl.bocadopolar.clientes.dto.ClienteDtos.PreferenciaRequest;
import cl.bocadopolar.clientes.exception.RecursoNoEncontradoException;
import cl.bocadopolar.clientes.exception.ReglaNegocioException;
import cl.bocadopolar.clientes.exception.SolicitudInvalidaException;
import cl.bocadopolar.clientes.model.Cliente;
import cl.bocadopolar.clientes.model.DireccionEntrega;
import cl.bocadopolar.clientes.model.MedioPago;
import cl.bocadopolar.clientes.model.NotaCliente;
import cl.bocadopolar.clientes.model.PreferenciaCliente;
import cl.bocadopolar.clientes.model.SegmentoCliente;
import cl.bocadopolar.clientes.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repository;
    private final ConfigClient configClient;

    /* ---------- Ficha ---------- */

    public List<Cliente> listar(SegmentoCliente segmento, String q) {
        String filtro = q == null ? "" : q.trim().toLowerCase();
        return repository.findAll().stream()
                .filter(c -> segmento == null || c.getSegmento() == segmento)
                .filter(c -> filtro.isEmpty() || c.getEmail().toLowerCase().contains(filtro)
                        || c.nombreCompleto().toLowerCase().contains(filtro))
                .sorted(Comparator.comparing(Cliente::getGastoTotal).reversed())
                .toList();
    }

    public Cliente obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el cliente " + id));
    }

    public Cliente porUsuario(Long usuarioId) {
        return repository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario " + usuarioId + " no tiene ficha de cliente"));
    }

    /** Lo llama ms-auth al registrar: devuelve la ficha existente o crea una nueva. */
    @Transactional
    public Cliente obtenerOCrearDesdeUsuario(ClienteRequest req) {
        if (req.usuarioId() != null) {
            var existente = repository.findByUsuarioId(req.usuarioId());
            if (existente.isPresent()) {
                return existente.get();
            }
        }
        var porEmail = repository.findByEmailIgnoreCase(req.email().trim());
        if (porEmail.isPresent()) {
            Cliente c = porEmail.get();
            if (c.getUsuarioId() == null) {
                c.setUsuarioId(req.usuarioId());
            }
            return repository.save(c);
        }
        return repository.save(nuevo(req));
    }

    /** Alta manual desde el CRM (por ejemplo un cliente B2B). */
    @Transactional
    public Cliente crear(ClienteRequest req) {
        if (repository.findByEmailIgnoreCase(req.email().trim()).isPresent()) {
            throw new ReglaNegocioException("Ya existe un cliente con el correo " + req.email());
        }
        return repository.save(nuevo(req));
    }

    @Transactional
    public Cliente actualizarPerfil(Long id, PerfilRequest req) {
        Cliente c = obtener(id);
        c.setNombre(req.nombre().trim());
        c.setApellido(vacioANull(req.apellido()));
        c.setTelefono(vacioANull(req.telefono()));
        c.setRut(vacioANull(req.rut()));
        return repository.save(c);
    }

    /* ---------- Direcciones ---------- */

    public List<DireccionEntrega> direcciones(Long clienteId) {
        return obtener(clienteId).getDirecciones();
    }

    @Transactional
    public DireccionEntrega agregarDireccion(Long clienteId, DireccionRequest req) {
        Cliente c = obtener(clienteId);
        if (c.getDirecciones().size() >= 10) {
            throw new ReglaNegocioException("Máximo 10 direcciones por cliente");
        }
        boolean principal = c.getDirecciones().isEmpty() || Boolean.TRUE.equals(req.principal());
        DireccionEntrega d = new DireccionEntrega(null, req.alias().trim(), req.calle().trim(), req.comuna().trim(),
                vacioANull(req.referencia()), principal);
        if (principal) {
            c.getDirecciones().forEach(x -> x.setPrincipal(false));
        }
        c.getDirecciones().add(d);
        repository.flush();
        return d;
    }

    @Transactional
    public DireccionEntrega actualizarDireccion(Long clienteId, Long direccionId, DireccionRequest req) {
        Cliente c = obtener(clienteId);
        DireccionEntrega d = buscarDireccion(c, direccionId);
        d.setAlias(req.alias().trim());
        d.setCalle(req.calle().trim());
        d.setComuna(req.comuna().trim());
        d.setReferencia(vacioANull(req.referencia()));
        if (Boolean.TRUE.equals(req.principal())) {
            c.getDirecciones().forEach(x -> x.setPrincipal(x == d));
        }
        repository.save(c);
        return d;
    }

    @Transactional
    public DireccionEntrega marcarPrincipal(Long clienteId, Long direccionId) {
        Cliente c = obtener(clienteId);
        DireccionEntrega d = buscarDireccion(c, direccionId);
        c.getDirecciones().forEach(x -> x.setPrincipal(x == d));
        repository.save(c);
        return d;
    }

    @Transactional
    public void eliminarDireccion(Long clienteId, Long direccionId) {
        Cliente c = obtener(clienteId);
        DireccionEntrega d = buscarDireccion(c, direccionId);
        c.getDirecciones().remove(d);
        if (Boolean.TRUE.equals(d.getPrincipal()) && !c.getDirecciones().isEmpty()) {
            c.getDirecciones().get(0).setPrincipal(true);
        }
        repository.save(c);
    }

    /* ---------- Medios de pago ---------- */

    public List<MedioPago> mediosPago(Long clienteId) {
        return obtener(clienteId).getMediosPago();
    }

    @Transactional
    public MedioPago agregarMedioPago(Long clienteId, MedioPagoRequest req) {
        Cliente c = obtener(clienteId);
        YearMonth vence = YearMonth.parse(req.vence(), DateTimeFormatter.ofPattern("MM/yy"));
        if (vence.isBefore(YearMonth.now())) {
            throw new SolicitudInvalidaException("La tarjeta está vencida");
        }
        MedioPago m = new MedioPago(null, req.alias().trim(), req.marca().trim(), req.ultimos4(), req.tokenPasarela(),
                req.vence());
        c.getMediosPago().add(m);
        repository.flush();
        return m;
    }

    @Transactional
    public void eliminarMedioPago(Long clienteId, Long medioId) {
        Cliente c = obtener(clienteId);
        boolean quitado = c.getMediosPago().removeIf(m -> m.getId().equals(medioId));
        if (!quitado) {
            throw new RecursoNoEncontradoException("El cliente no tiene el medio de pago " + medioId);
        }
        repository.save(c);
    }

    /* ---------- Preferencias ---------- */

    public PreferenciaCliente preferencias(Long clienteId) {
        return obtener(clienteId).getPreferencia();
    }

    @Transactional
    public PreferenciaCliente actualizarPreferencias(Long clienteId, PreferenciaRequest req) {
        Cliente c = obtener(clienteId);
        if (c.getPreferencia() == null) {
            c.setPreferencia(new PreferenciaCliente());
        }
        c.getPreferencia().setAlergias(vacioANull(req.alergias()));
        c.getPreferencia().setNotas(vacioANull(req.notas()));
        repository.flush();
        return c.getPreferencia();
    }

    /* ---------- Notas CRM ---------- */

    public List<NotaCliente> notas(Long clienteId) {
        return obtener(clienteId).getNotas();
    }

    @Transactional
    public NotaCliente agregarNota(Long clienteId, NotaRequest req) {
        Cliente c = obtener(clienteId);
        NotaCliente n = new NotaCliente(null, req.tipo(), req.texto().trim(), req.autorId(), Instant.now());
        c.getNotas().add(0, n);
        repository.flush();
        return n;
    }

    /* ---------- Compras y segmentación ---------- */

    /** Suma una compra entregada y recalcula el segmento (VIP / Frecuente / Nuevo). */
    @Transactional
    public Cliente registrarCompra(Long clienteId, BigDecimal monto) {
        Cliente c = obtener(clienteId);
        c.setGastoTotal(c.getGastoTotal().add(monto));
        c.setTotalPedidos(c.getTotalPedidos() + 1);
        c.setSegmento(calcularSegmento(c));
        return repository.save(c);
    }

    @Transactional
    public List<Cliente> recalcularSegmentos() {
        List<Cliente> todos = repository.findAll();
        todos.forEach(c -> c.setSegmento(calcularSegmento(c)));
        return repository.saveAll(todos);
    }

    SegmentoCliente calcularSegmento(Cliente c) {
        BigDecimal montoVip = configClient.regla("VIP_MONTO", new BigDecimal("500000"));
        int pedidosFrecuente = configClient.regla("FRECUENTE_PEDIDOS", new BigDecimal("3")).intValue();
        if (c.getGastoTotal().compareTo(montoVip) >= 0) {
            return SegmentoCliente.VIP;
        }
        if (c.getTotalPedidos() >= pedidosFrecuente) {
            return SegmentoCliente.FRECUENTE;
        }
        return SegmentoCliente.NUEVO;
    }

    /* ---------- utilidades ---------- */

    private Cliente nuevo(ClienteRequest req) {
        Cliente c = new Cliente();
        c.setUsuarioId(req.usuarioId());
        c.setNombre(req.nombre().trim());
        c.setApellido(vacioANull(req.apellido()));
        c.setEmail(req.email().trim().toLowerCase());
        c.setTelefono(vacioANull(req.telefono()));
        c.setRut(vacioANull(req.rut()));
        c.setCreadoEn(Instant.now());
        return c;
    }

    private DireccionEntrega buscarDireccion(Cliente c, Long direccionId) {
        return c.getDirecciones().stream()
                .filter(d -> d.getId().equals(direccionId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("El cliente no tiene la dirección " + direccionId));
    }

    private static String vacioANull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
