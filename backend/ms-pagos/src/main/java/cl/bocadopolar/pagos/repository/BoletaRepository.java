package cl.bocadopolar.pagos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.pagos.model.Boleta;

@Repository
public interface BoletaRepository extends JpaRepository<Boleta, Long> {

    Optional<Boleta> findByPagoId(Long pagoId);

    List<Boleta> findByPedidoIdOrderByEmisionDesc(Long pedidoId);
}
