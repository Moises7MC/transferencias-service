package pe.jllalle.transferenciasservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import pe.jllalle.transferenciasservice.domain.Cuenta;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    /**
     * Bloqueo pesimista: al leer la cuenta para transferir, la fila queda bloqueada
     * hasta que termine la transacción, evitando que dos transferencias simultáneas
     * lean el mismo saldo y ambas lo den por válido (condición de carrera).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Cuenta> findWithLockById(Long id);
}
