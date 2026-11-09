package cl.bocadopolar.marketing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.marketing.model.CarritoAbandonado;

@Repository
public interface CarritoAbandonadoRepository extends JpaRepository<CarritoAbandonado, Long> {

    boolean existsByCarritoIdAndRecuperadoFalse(Long carritoId);

    List<CarritoAbandonado> findAllByOrderByFechaDesc();
}
