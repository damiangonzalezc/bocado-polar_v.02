package cl.bocadopolar.reportes.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.reportes.model.VisitaDiaria;

@Repository
public interface VisitaDiariaRepository extends JpaRepository<VisitaDiaria, LocalDate> {

    List<VisitaDiaria> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);
}
