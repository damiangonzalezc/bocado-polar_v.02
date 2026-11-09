package cl.bocadopolar.reparto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.reparto.model.CierreCaja;

@Repository
public interface CierreCajaRepository extends JpaRepository<CierreCaja, Long> {

    java.util.List<CierreCaja> findByRepartidorIdOrderByFechaDesc(Long repartidorId);
    java.util.List<CierreCaja> findAllByOrderByFechaDesc();
}
