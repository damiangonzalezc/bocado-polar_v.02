package cl.bocadopolar.catalogo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bocadopolar.catalogo.client.InventarioClient;
import cl.bocadopolar.catalogo.dto.CatalogoDtos.AtributoRequest;
import cl.bocadopolar.catalogo.dto.CatalogoDtos.CategoriaRequest;
import cl.bocadopolar.catalogo.dto.CatalogoDtos.ProductoRequest;
import cl.bocadopolar.catalogo.dto.CatalogoDtos.ResumenValoracion;
import cl.bocadopolar.catalogo.exception.RecursoNoEncontradoException;
import cl.bocadopolar.catalogo.exception.ReglaNegocioException;
import cl.bocadopolar.catalogo.exception.SolicitudInvalidaException;
import cl.bocadopolar.catalogo.model.Atributo;
import cl.bocadopolar.catalogo.model.Categoria;
import cl.bocadopolar.catalogo.model.Producto;
import cl.bocadopolar.catalogo.model.Valoracion;
import cl.bocadopolar.catalogo.repository.AtributoRepository;
import cl.bocadopolar.catalogo.repository.CategoriaRepository;
import cl.bocadopolar.catalogo.repository.ProductoRepository;
import cl.bocadopolar.catalogo.repository.ValoracionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final AtributoRepository atributoRepository;
    private final ValoracionRepository valoracionRepository;
    private final InventarioClient inventarioClient;

    /* =============== Productos =============== */

    /**
     * CU-03.01 Buscar y filtrar. orden: nombre | precio_asc | precio_desc | valoracion.
     */
    public List<Producto> buscar(String q, Long categoriaId, BigDecimal precioMin, BigDecimal precioMax,
            boolean incluirInactivos, String orden) {
        String filtro = normalizar(q);
        Stream<Producto> s = productoRepository.findAll().stream()
                .filter(p -> incluirInactivos || Boolean.TRUE.equals(p.getActivo()))
                .filter(p -> categoriaId == null || p.getCategoria().getId().equals(categoriaId))
                .filter(p -> precioMin == null || p.getPrecioKg().compareTo(precioMin) >= 0)
                .filter(p -> precioMax == null || p.getPrecioKg().compareTo(precioMax) <= 0)
                .filter(p -> filtro.isEmpty() || normalizar(p.getNombre()).contains(filtro)
                        || normalizar(p.getSku()).contains(filtro) || normalizar(p.getOrigen()).contains(filtro)
                        || normalizar(p.getCategoria().getNombre()).contains(filtro));
        Comparator<Producto> cmp = switch (orden == null ? "nombre" : orden) {
            case "precio_asc" -> Comparator.comparing(Producto::getPrecioKg);
            case "precio_desc" -> Comparator.comparing(Producto::getPrecioKg).reversed();
            case "valoracion" -> Comparator.comparing(Producto::getValoracionProm).reversed();
            default -> Comparator.comparing(Producto::getNombre);
        };
        return s.sorted(cmp).toList();
    }

    public Producto obtener(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + id));
    }

    public Producto porSku(String sku) {
        return productoRepository.findBySkuIgnoreCase(sku)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto con SKU " + sku));
    }

    /** Lo usa ms-pedidos para tomar nombre y precio vigentes. */
    public List<Producto> porIds(List<Long> ids) {
        return productoRepository.findAllById(ids);
    }

    @Transactional
    public Producto crear(ProductoRequest req) {
        if (productoRepository.existsBySkuIgnoreCase(req.sku())) {
            throw new ReglaNegocioException("Ya existe un producto con SKU " + req.sku());
        }
        Producto p = new Producto();
        copiar(req, p);
        return productoRepository.save(p);
    }

    @Transactional
    public Producto actualizar(Long id, ProductoRequest req) {
        Producto p = obtener(id);
        productoRepository.findBySkuIgnoreCase(req.sku())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ReglaNegocioException("El SKU " + req.sku() + " ya lo usa otro producto");
                });
        copiar(req, p);
        return productoRepository.save(p);
    }

    @Transactional
    public Producto cambiarActivo(Long id, boolean activo) {
        Producto p = obtener(id);
        p.setActivo(activo);
        return productoRepository.save(p);
    }

    /** Solo se elimina si ya no tiene kilos en bodega; si no, conviene desactivarlo. */
    @Transactional
    public void eliminar(Long id) {
        Producto p = obtener(id);
        BigDecimal stock = inventarioClient.stockKg(id);
        if (stock != null && stock.signum() > 0) {
            throw new ReglaNegocioException("El producto aún tiene " + stock + " kg en bodega. Desactívalo en vez de eliminarlo.");
        }
        valoracionRepository.deleteAll(valoracionRepository.findByProductoId(id));
        productoRepository.delete(p);
    }

    /** CU-03.05 Ajuste masivo de precios (redondeado a pesos). */
    @Transactional
    public List<Producto> ajustarPreciosMasivo(List<Long> ids, BigDecimal porcentaje) {
        List<Producto> productos = ids == null || ids.isEmpty() ? productoRepository.findAll()
                : productoRepository.findAllById(ids);
        BigDecimal factor = BigDecimal.ONE.add(porcentaje.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        productos.forEach(p -> {
            BigDecimal nuevo = p.getPrecioKg().multiply(factor).setScale(0, RoundingMode.HALF_UP);
            if (nuevo.signum() <= 0) {
                throw new SolicitudInvalidaException("El ajuste dejaría el precio de " + p.getNombre() + " en 0 o menos");
            }
            p.setPrecioKg(nuevo);
        });
        return productoRepository.saveAll(productos);
    }

    /* =============== Valoraciones =============== */

    /** CU-03.03 Valorar con estrellas de 0,5 a 5 (en pasos de 0,5). Un voto por cliente. */
    @Transactional
    public ResumenValoracion valorar(Long productoId, Long clienteId, Double estrellas) {
        if (estrellas == null || estrellas < 0.5 || estrellas > 5 || (estrellas * 2) % 1 != 0) {
            throw new SolicitudInvalidaException("Las estrellas van de 0,5 a 5 en pasos de 0,5");
        }
        Producto p = obtener(productoId);
        double suma = p.getValoracionProm() * p.getValoracionVotos();
        int votos = p.getValoracionVotos();
        var existente = valoracionRepository.findByProductoIdAndClienteId(productoId, clienteId);
        if (existente.isPresent()) {
            Valoracion v = existente.get();
            suma += estrellas - v.getEstrellas();
            v.setEstrellas(estrellas);
            v.setFecha(Instant.now());
            valoracionRepository.save(v);
        } else {
            suma += estrellas;
            votos++;
            valoracionRepository.save(new Valoracion(null, productoId, clienteId, estrellas, Instant.now()));
        }
        p.setValoracionVotos(votos);
        p.setValoracionProm(votos == 0 ? 0.0 : suma / votos);
        productoRepository.save(p);
        return new ResumenValoracion(productoId, p.getValoracion(), votos, estrellas);
    }

    public ResumenValoracion resumen(Long productoId, Long clienteId) {
        Producto p = obtener(productoId);
        Double mia = clienteId == null ? 0.0
                : valoracionRepository.findByProductoIdAndClienteId(productoId, clienteId)
                        .map(Valoracion::getEstrellas).orElse(0.0);
        return new ResumenValoracion(productoId, p.getValoracion(), p.getValoracionVotos(), mia);
    }

    /* =============== Categorías =============== */

    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }

    public Categoria obtenerCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría " + id));
    }

    @Transactional
    public Categoria crearCategoria(CategoriaRequest req) {
        if (categoriaRepository.findByNombreIgnoreCase(req.nombre().trim()).isPresent()) {
            throw new ReglaNegocioException("Ya existe la categoría " + req.nombre());
        }
        return categoriaRepository.save(new Categoria(null, req.nombre().trim(), req.descripcion()));
    }

    @Transactional
    public Categoria actualizarCategoria(Long id, CategoriaRequest req) {
        Categoria c = obtenerCategoria(id);
        categoriaRepository.findByNombreIgnoreCase(req.nombre().trim())
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new ReglaNegocioException("Ya existe la categoría " + req.nombre());
                });
        c.setNombre(req.nombre().trim());
        c.setDescripcion(req.descripcion());
        return categoriaRepository.save(c);
    }

    @Transactional
    public void eliminarCategoria(Long id) {
        Categoria c = obtenerCategoria(id);
        long enUso = productoRepository.countByCategoriaId(id);
        if (enUso > 0) {
            throw new ReglaNegocioException("La categoría tiene " + enUso + " producto(s). Muévelos antes de eliminarla.");
        }
        categoriaRepository.delete(c);
    }

    /* =============== Atributos =============== */

    public List<Atributo> listarAtributos() {
        return atributoRepository.findAll();
    }

    public Atributo obtenerAtributo(Long id) {
        return atributoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el atributo " + id));
    }

    @Transactional
    public Atributo crearAtributo(AtributoRequest req) {
        if (atributoRepository.findByNombreIgnoreCase(req.nombre().trim()).isPresent()) {
            throw new ReglaNegocioException("Ya existe el atributo " + req.nombre());
        }
        return atributoRepository.save(new Atributo(req.nombre().trim(), req.valores() == null ? List.of() : req.valores()));
    }

    @Transactional
    public Atributo actualizarAtributo(Long id, AtributoRequest req) {
        Atributo a = obtenerAtributo(id);
        a.setNombre(req.nombre().trim());
        a.getValores().clear();
        if (req.valores() != null) {
            a.getValores().addAll(req.valores());
        }
        return atributoRepository.save(a);
    }

    @Transactional
    public Atributo agregarValor(Long id, String valor) {
        Atributo a = obtenerAtributo(id);
        if (a.getValores().stream().anyMatch(v -> v.equalsIgnoreCase(valor.trim()))) {
            throw new ReglaNegocioException("El valor " + valor + " ya existe en " + a.getNombre());
        }
        a.getValores().add(valor.trim());
        return atributoRepository.save(a);
    }

    @Transactional
    public void eliminarAtributo(Long id) {
        atributoRepository.delete(obtenerAtributo(id));
    }

    /* =============== utilidades =============== */

    private void copiar(ProductoRequest req, Producto p) {
        Categoria categoria = obtenerCategoria(req.categoriaId());
        p.setSku(req.sku().trim().toUpperCase());
        p.setNombre(req.nombre().trim());
        p.setDescripcion(req.descripcion());
        p.setCategoria(categoria);
        p.setOrigen(req.origen());
        p.setAlergenos(req.alergenos());
        p.setImagen(req.imagen());
        if (req.activo() != null) {
            p.setActivo(req.activo());
        }
        p.setPrecioKg(req.precioKg());
        p.setCostoKg(req.costoKg());
        p.getAtributos().clear();
        if (req.atributos() != null) {
            p.getAtributos().putAll(new LinkedHashMap<>(req.atributos()));
        }
    }

    static String normalizar(String t) {
        if (t == null) {
            return "";
        }
        return Normalizer.normalize(t.trim().toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
