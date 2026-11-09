package cl.bocadopolar.inventario.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.inventario.model.Lote;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {

    List<Lote> findByProductoIdOrderByVencimientoAsc(Long productoId);

    boolean existsByCodigoLoteIgnoreCase(String codigoLote);
}
