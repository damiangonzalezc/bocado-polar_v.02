package cl.bocadopolar.reportes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.reportes.model.KpiPeriodo;

@Repository
public interface KpiPeriodoRepository extends JpaRepository<KpiPeriodo, Long> {

    List<KpiPeriodo> findAllByOrderByCalculadoEnDesc();
}
