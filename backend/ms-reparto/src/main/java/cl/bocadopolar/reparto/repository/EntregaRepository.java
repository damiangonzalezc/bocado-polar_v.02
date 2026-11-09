package cl.bocadopolar.reparto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.reparto.model.Entrega;

@Repository
public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    java.util.List<Entrega> findByRepartidorIdOrderByFechaDesc(Long repartidorId);
    java.util.List<Entrega> findByTurnoId(Long turnoId);
    java.util.List<Entrega> findByPedidoIdOrderByFechaDesc(Long pedidoId);
}
