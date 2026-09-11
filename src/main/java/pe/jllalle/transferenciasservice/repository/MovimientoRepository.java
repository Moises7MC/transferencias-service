package pe.jllalle.transferenciasservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.jllalle.transferenciasservice.domain.Movimiento;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {
}
