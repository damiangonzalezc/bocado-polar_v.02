package cl.bocadopolar.reparto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.reparto.model.Incidencia;

@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    java.util.List<Incidencia> findByReportadaPorIdOrderByFechaDesc(Long reportadaPorId);
    java.util.List<Incidencia> findAllByOrderByFechaDesc();
}
