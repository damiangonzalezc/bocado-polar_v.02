package cl.bocadopolar.pagos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.pagos.model.EstadoReembolso;
import cl.bocadopolar.pagos.model.Reembolso;

@Repository
public interface ReembolsoRepository extends JpaRepository<Reembolso, Long> {

    List<Reembolso> findByEstadoOrderByFechaDesc(EstadoReembolso estado);

    List<Reembolso> findAllByOrderByFechaDesc();

    boolean existsByPagoIdAndEstado(Long pagoId, EstadoReembolso estado);
}
