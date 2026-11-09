package cl.bocadopolar.rutas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.rutas.model.ParadaRuta;

@Repository
public interface ParadaRutaRepository extends JpaRepository<ParadaRuta, Long> {
}
