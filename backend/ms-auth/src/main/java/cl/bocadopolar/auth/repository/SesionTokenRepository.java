package cl.bocadopolar.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.auth.model.SesionToken;

@Repository
public interface SesionTokenRepository extends JpaRepository<SesionToken, Long> {

    Optional<SesionToken> findByJti(String jti);

    List<SesionToken> findByUsuarioIdAndRevocadoFalse(Long usuarioId);
}
