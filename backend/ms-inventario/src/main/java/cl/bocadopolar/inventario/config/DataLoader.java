package cl.bocadopolar.inventario.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cl.bocadopolar.inventario.model.Lote;
import cl.bocadopolar.inventario.model.MovimientoStock;
import cl.bocadopolar.inventario.model.TipoMovimiento;
import cl.bocadopolar.inventario.repository.LoteRepository;
import cl.bocadopolar.inventario.repository.MovimientoStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Lotes del prototipo para los productos 1..6 de ms-catalogo (Mix Mariscos queda en stock crítico). */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bocado.semilla.habilitada", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {

    private final LoteRepository loteRepository;
    private final MovimientoStockRepository movimientoRepository;

    @Override
    public void run(String... args) {
        if (loteRepository.count() > 0) {
            return;
        }
        lote(1L, "Salmón Premium", "SAL-2609-A", "60", 12, -18.0);
        lote(1L, "Salmón Premium", "SAL-2610-B", "26", 25, -18.0);
        lote(2L, "Filete de Reineta", "REI-2609-B", "54", 9, -18.0);
        lote(3L, "Camarón Pelado", "CAM-2609-C", "42", 7, -17.0);
        lote(4L, "Pulpo Cocido", "PUL-2609-D", "31", 15, -16.0);
        lote(5L, "Merluza Austral", "MER-2609-E", "75", 19, -18.0);
        lote(6L, "Mix Mariscos", "MIX-2609-F", "28", 5, -14.0);
        log.info("ms-inventario: lotes de ejemplo creados");
    }

    private void lote(Long productoId, String nombre, String codigo, String kg, int dias, Double temp) {
        Lote l = new Lote();
        l.setProductoId(productoId);
        l.setProductoNombre(nombre);
        l.setCodigoLote(codigo);
        l.setStockKg(new BigDecimal(kg));
        l.setReservadoKg(BigDecimal.ZERO);
        l.setVencimiento(LocalDate.now().plusDays(dias));
        l.setTempAlmacenaje(temp);
        l.setBloqueado(false);
        l.setIngreso(Instant.now());
        l = loteRepository.save(l);
        movimientoRepository.save(new MovimientoStock(null, l.getId(), productoId, TipoMovimiento.ENTRADA,
                "Carga inicial", l.getStockKg(), Instant.now(), 3L));
    }
}
