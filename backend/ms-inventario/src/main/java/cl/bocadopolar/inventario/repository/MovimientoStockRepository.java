package cl.bocadopolar.inventario.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.inventario.model.MovimientoStock;

@Repository
public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    List<MovimientoStock> findByLoteIdOrderByFechaDesc(Long loteId);

    List<MovimientoStock> findByProductoIdOrderByFechaDesc(Long productoId);

    List<MovimientoStock> findTop200ByOrderByFechaDesc();
}
