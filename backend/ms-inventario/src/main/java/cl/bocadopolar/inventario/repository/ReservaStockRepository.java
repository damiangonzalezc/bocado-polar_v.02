package cl.bocadopolar.inventario.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.inventario.model.EstadoReserva;
import cl.bocadopolar.inventario.model.ReservaStock;

@Repository
public interface ReservaStockRepository extends JpaRepository<ReservaStock, Long> {

    List<ReservaStock> findByPedidoId(Long pedidoId);

    List<ReservaStock> findByEstadoAndExpiraEnBefore(EstadoReserva estado, Instant limite);

    boolean existsByLoteIdAndEstado(Long loteId, EstadoReserva estado);
}
