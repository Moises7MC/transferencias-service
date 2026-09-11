package pe.jllalle.transferenciasservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.jllalle.transferenciasservice.domain.Transferencia;

import java.util.Optional;

public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {

    Optional<Transferencia> findByClaveIdempotencia(String claveIdempotencia);
}
