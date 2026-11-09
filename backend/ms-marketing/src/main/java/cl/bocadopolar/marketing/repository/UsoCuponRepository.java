package cl.bocadopolar.marketing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.marketing.model.UsoCupon;

@Repository
public interface UsoCuponRepository extends JpaRepository<UsoCupon, Long> {

    long countByCuponIdAndClienteId(Long cuponId, Long clienteId);

    boolean existsByPedidoId(Long pedidoId);

    List<UsoCupon> findByCuponIdOrderByFechaDesc(Long cuponId);
}
