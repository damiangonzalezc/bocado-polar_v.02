package cl.bocadopolar.pagos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.pagos.model.Pago;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByIdempotencyKey(String idempotencyKey);

    List<Pago> findByPedidoIdOrderByFechaDesc(Long pedidoId);
}
