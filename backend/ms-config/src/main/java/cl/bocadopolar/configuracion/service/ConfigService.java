package cl.bocadopolar.configuracion.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bocadopolar.configuracion.dto.ConfigDtos.ConfiguracionRequest;
import cl.bocadopolar.configuracion.dto.ConfigDtos.EmpresaRequest;
import cl.bocadopolar.configuracion.dto.ConfigDtos.ReglaRequest;
import cl.bocadopolar.configuracion.dto.ConfigDtos.ReglaValorRequest;
import cl.bocadopolar.configuracion.dto.ConfigDtos.ZonaRequest;
import cl.bocadopolar.configuracion.exception.RecursoNoEncontradoException;
import cl.bocadopolar.configuracion.exception.ReglaNegocioException;
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

@Service
@RequiredArgsConstructor
public class ConfigService {

    private final ConfiguracionGlobalRepository configuracionRepository;
    private final ZonaEnvioRepository zonaRepository;
    private final EmpresaLogisticaRepository empresaRepository;
    private final PasarelaPagoRepository pasarelaRepository;
    private final ReglaNegocioRepository reglaRepository;

    /* ---------- Configuración global ---------- */

    public ConfiguracionGlobal obtenerConfiguracion() {
        return configuracionRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay configuración global cargada"));
    }

    @Transactional
    public ConfiguracionGlobal actualizarConfiguracion(ConfiguracionRequest req) {
        ConfiguracionGlobal c = configuracionRepository.findFirstByOrderByIdAsc().orElseGet(ConfiguracionGlobal::new);
        c.setPais(req.pais().trim());
        c.setMoneda(req.moneda().trim().toUpperCase());
        c.setPreciosConIva(req.preciosConIva());
        c.setIva(req.iva());
        c.setVigenteDesde(LocalDate.now());
        return configuracionRepository.save(c);
    }

    public BigDecimal obtenerIva() {
        return obtenerConfiguracion().getIva();
    }

    /* ---------- Zonas de envío ---------- */

    public List<ZonaEnvio> listarZonas(Boolean soloActivas) {
        return Boolean.TRUE.equals(soloActivas) ? zonaRepository.findByActivaTrueOrderByComunaAsc() : zonaRepository.findAll();
    }

    public ZonaEnvio obtenerZona(Long id) {
        return zonaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la zona " + id));
    }

    @Transactional
    public ZonaEnvio crearZona(ZonaRequest req) {
        if (zonaRepository.existsByComunaIgnoreCase(req.comuna().trim())) {
            throw new ReglaNegocioException("La comuna " + req.comuna() + " ya tiene zona de envío");
        }
        ZonaEnvio z = new ZonaEnvio(null, req.comuna().trim(), req.tarifa(), req.activa() == null || req.activa());
        return zonaRepository.save(z);
    }

    @Transactional
    public ZonaEnvio actualizarZona(Long id, ZonaRequest req) {
        ZonaEnvio z = obtenerZona(id);
        zonaRepository.findByComunaIgnoreCase(req.comuna().trim())
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new ReglaNegocioException("La comuna " + req.comuna() + " ya tiene zona de envío");
                });
        z.setComuna(req.comuna().trim());
        z.setTarifa(req.tarifa());
        if (req.activa() != null) {
            z.setActiva(req.activa());
        }
        return zonaRepository.save(z);
    }

    @Transactional
    public ZonaEnvio cambiarZonaActiva(Long id, boolean activa) {
        ZonaEnvio z = obtenerZona(id);
        z.setActiva(activa);
        return zonaRepository.save(z);
    }

    @Transactional
    public void eliminarZona(Long id) {
        zonaRepository.delete(obtenerZona(id));
    }

    /** Tarifa de envío de una comuna activa (la usa ms-pedidos en el checkout). */
    public ZonaEnvio tarifaPara(String comuna) {
        return zonaRepository.findByComunaIgnoreCase(comuna.trim())
                .filter(ZonaEnvio::getActiva)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay despacho activo para la comuna " + comuna));
    }

    /* ---------- Empresas de logística ---------- */

    public List<EmpresaLogistica> listarEmpresas() {
        return empresaRepository.findAll();
    }

    public EmpresaLogistica obtenerEmpresa(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la empresa de logística " + id));
    }

    @Transactional
    public EmpresaLogistica crearEmpresa(EmpresaRequest req) {
        return empresaRepository.save(new EmpresaLogistica(null, req.nombre().trim(), req.tipo().trim(),
                req.activa() != null && req.activa()));
    }

    @Transactional
    public EmpresaLogistica actualizarEmpresa(Long id, EmpresaRequest req) {
        EmpresaLogistica e = obtenerEmpresa(id);
        e.setNombre(req.nombre().trim());
        e.setTipo(req.tipo().trim());
        if (req.activa() != null) {
            e.setActiva(req.activa());
        }
        return empresaRepository.save(e);
    }

    @Transactional
    public EmpresaLogistica cambiarEmpresaActiva(Long id, boolean activa) {
        EmpresaLogistica e = obtenerEmpresa(id);
        e.setActiva(activa);
        return empresaRepository.save(e);
    }

    @Transactional
    public void eliminarEmpresa(Long id) {
        empresaRepository.delete(obtenerEmpresa(id));
    }

    /* ---------- Pasarelas de pago ---------- */

    public List<PasarelaPago> listarPasarelas() {
        return pasarelaRepository.findAll();
    }

    @Transactional
    public PasarelaPago cambiarConexion(Long id, boolean conectada) {
        PasarelaPago p = pasarelaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la pasarela " + id));
        p.setConectada(conectada);
        return pasarelaRepository.save(p);
    }

    public boolean pasarelaDisponible(String nombre) {
        return pasarelaRepository.findByNombreIgnoreCase(nombre.trim())
                .map(PasarelaPago::getConectada)
                .orElse(false);
    }

    /* ---------- Reglas de negocio ---------- */

    public List<ReglaNegocio> listarReglas() {
        return reglaRepository.findAll();
    }

    public ReglaNegocio obtenerRegla(String codigo) {
        return reglaRepository.findById(codigo.toUpperCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la regla " + codigo));
    }

    @Transactional
    public ReglaNegocio crearRegla(ReglaRequest req) {
        if (reglaRepository.existsById(req.codigo())) {
            throw new ReglaNegocioException("La regla " + req.codigo() + " ya existe");
        }
        return reglaRepository.save(new ReglaNegocio(req.codigo(), req.descripcion(), req.valor(), req.unidad(), LocalDate.now()));
    }

    @Transactional
    public ReglaNegocio actualizarRegla(String codigo, ReglaValorRequest req) {
        ReglaNegocio r = obtenerRegla(codigo);
        r.setValor(req.valor());
        if (req.descripcion() != null && !req.descripcion().isBlank()) {
            r.setDescripcion(req.descripcion());
        }
        if (req.unidad() != null) {
            r.setUnidad(req.unidad());
        }
        r.setVigenteDesde(LocalDate.now());
        return reglaRepository.save(r);
    }

    @Transactional
    public void eliminarRegla(String codigo) {
        reglaRepository.delete(obtenerRegla(codigo));
    }
}
