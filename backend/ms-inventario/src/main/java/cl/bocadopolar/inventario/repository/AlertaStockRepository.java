package cl.bocadopolar.inventario.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.inventario.model.AlertaStock;

@Repository
public interface AlertaStockRepository extends JpaRepository<AlertaStock, Long> {

    List<AlertaStock> findByAtendidaOrderByFechaDesc(Boolean atendida);

    List<AlertaStock> findByProductoIdAndAtendidaFalse(Long productoId);

    List<AlertaStock> findAllByOrderByFechaDesc();
}
