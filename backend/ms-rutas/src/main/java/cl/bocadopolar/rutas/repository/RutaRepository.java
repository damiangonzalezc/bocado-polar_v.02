package cl.bocadopolar.rutas.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.rutas.model.Ruta;

@Repository
public interface RutaRepository extends JpaRepository<Ruta, Long> {

    List<Ruta> findByRepartidorIdAndFecha(Long repartidorId, LocalDate fecha);

    List<Ruta> findByFecha(LocalDate fecha);
}
