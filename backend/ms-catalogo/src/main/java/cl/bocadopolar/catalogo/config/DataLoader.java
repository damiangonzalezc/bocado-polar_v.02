package cl.bocadopolar.catalogo.config;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cl.bocadopolar.catalogo.model.Atributo;
import cl.bocadopolar.catalogo.model.Categoria;
import cl.bocadopolar.catalogo.model.Producto;
import cl.bocadopolar.catalogo.repository.AtributoRepository;
import cl.bocadopolar.catalogo.repository.CategoriaRepository;
import cl.bocadopolar.catalogo.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Catálogo del prototipo: productos 1..6 (los IDs los usan ms-inventario y ms-pedidos en sus datos). */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bocado.semilla.habilitada", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final AtributoRepository atributoRepository;

    @Override
    public void run(String... args) {
        if (categoriaRepository.count() == 0) {
            categoriaRepository.saveAll(List.of(
                    new Categoria(null, "Pescados", "Filetes y piezas de pescado congelado."),
                    new Categoria(null, "Mariscos", "Crustáceos y moluscos."),
                    new Categoria(null, "Congelados", "Mezclas y preparados congelados.")));
        }
        if (atributoRepository.count() == 0) {
            atributoRepository.saveAll(List.of(
                    new Atributo("Calibre", List.of("Pequeño", "Mediano", "Grande")),
                    new Atributo("Marca", List.of("BOCADO Polar", "Pesca Sur")),
                    new Atributo("Formato", List.of("Bolsa 1 kg", "Caja 5 kg", "Caja 12,5 kg"))));
        }
        if (productoRepository.count() == 0) {
            Categoria pescados = categoriaRepository.findByNombreIgnoreCase("Pescados").orElseThrow();
            Categoria mariscos = categoriaRepository.findByNombreIgnoreCase("Mariscos").orElseThrow();
            Categoria congelados = categoriaRepository.findByNombreIgnoreCase("Congelados").orElseThrow();
            productoRepository.saveAll(List.of(
                    p("SAL-001", "Salmón Premium", pescados, 12990, 8900, "img/prod-salmon.svg", "Chiloé", "Pescado", 4.5, 38, "BOCADO Polar", "Caja 5 kg", "Grande"),
                    p("REI-002", "Filete de Reineta", pescados, 8990, 5900, "img/prod-reineta.svg", "Talcahuano", "Pescado", 4.0, 21, "Pesca Sur", "Bolsa 1 kg", "Mediano"),
                    p("CAM-003", "Camarón Pelado", mariscos, 10990, 7400, "img/prod-camaron.svg", "Coquimbo", "Crustáceos", 4.5, 33, "BOCADO Polar", "Bolsa 1 kg", "Mediano"),
                    p("PUL-004", "Pulpo Cocido", mariscos, 14990, 10200, "img/prod-pulpo.svg", "Lebu", "Moluscos", 3.5, 12, "Pesca Sur", "Caja 5 kg", "Grande"),
                    p("MER-005", "Merluza Austral", pescados, 6990, 4300, "img/prod-merluza.svg", "Aysén", "Pescado", 4.0, 27, "Pesca Sur", "Caja 12,5 kg", "Mediano"),
                    p("MIX-006", "Mix Mariscos", congelados, 11990, 8100, "img/prod-mix.svg", "Biobío", "Crustáceos, moluscos", 3.0, 9, "BOCADO Polar", "Bolsa 1 kg", "Pequeño")));
            log.info("ms-catalogo: productos de ejemplo creados");
        }
    }

    private Producto p(String sku, String nombre, Categoria cat, int precio, int costo, String img, String origen,
            String alergenos, double prom, int votos, String marca, String formato, String calibre) {
        Producto p = new Producto();
        p.setSku(sku);
        p.setNombre(nombre);
        p.setCategoria(cat);
        p.setPrecioKg(BigDecimal.valueOf(precio));
        p.setCostoKg(BigDecimal.valueOf(costo));
        p.setImagen(img);
        p.setOrigen(origen);
        p.setAlergenos(alergenos);
        p.setActivo(true);
        p.setValoracionProm(prom);
        p.setValoracionVotos(votos);
        Map<String, String> attr = new LinkedHashMap<>();
        attr.put("Marca", marca);
        attr.put("Formato", formato);
        attr.put("Calibre", calibre);
        p.setAtributos(attr);
        return p;
    }
}
