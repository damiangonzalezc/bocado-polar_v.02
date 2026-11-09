package cl.bocadopolar.marketing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.marketing.model.CanalCampana;

@Repository
public interface CanalCampanaRepository extends JpaRepository<CanalCampana, Long> {

    Optional<CanalCampana> findByCanalIgnoreCase(String canal);
}
