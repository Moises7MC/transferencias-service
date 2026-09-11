package pe.jllalle.transferenciasservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.jllalle.transferenciasservice.domain.Cuenta;
import pe.jllalle.transferenciasservice.domain.Movimiento;
import pe.jllalle.transferenciasservice.domain.TipoMovimiento;
import pe.jllalle.transferenciasservice.domain.Transferencia;
import pe.jllalle.transferenciasservice.dto.TransferenciaCommand;
import pe.jllalle.transferenciasservice.dto.TransferenciaResultado;
import pe.jllalle.transferenciasservice.repository.CuentaRepository;
import pe.jllalle.transferenciasservice.repository.MovimientoRepository;
import pe.jllalle.transferenciasservice.repository.TransferenciaRepository;
import pe.jllalle.transferenciasservice.service.exception.CuentaNoEncontradaException;
import pe.jllalle.transferenciasservice.service.exception.TransferenciaInvalidaException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor // Lombok genera el constructor con los 3 repositorios (inyección por constructor)
public class TransferenciaService {

    private final CuentaRepository cuentaRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final MovimientoRepository movimientoRepository;

    @Transactional
    public TransferenciaResultado transferir(TransferenciaCommand comando) {

        // 1) Idempotencia: si un cliente reintenta la misma petición (timeout, doble clic),
        //    no se debita dos veces; se devuelve el resultado que ya existe.
        var existente = transferenciaRepository.findByClaveIdempotencia(comando.claveIdempotencia());
        if (existente.isPresent()) {
            Transferencia previa = existente.get();
            return new TransferenciaResultado(
                    previa.getId(), previa.getClaveIdempotencia(), previa.getEstado(),
                    previa.getCuentaOrigen().getSaldo());
        }

        // 2) Validaciones de la operación, antes de tocar ninguna cuenta
        if (comando.cuentaOrigenId().equals(comando.cuentaDestinoId())) {
            throw new TransferenciaInvalidaException("No se puede transferir a la misma cuenta");
        }
        if (comando.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferenciaInvalidaException("El monto debe ser mayor a cero");
        }

        // 3) Bloqueo pesimista: evita que dos transferencias concurrentes lean el mismo saldo
        Cuenta origen = cuentaRepository.findWithLockById(comando.cuentaOrigenId())
                .orElseThrow(() -> new CuentaNoEncontradaException(comando.cuentaOrigenId()));
        Cuenta destino = cuentaRepository.findWithLockById(comando.cuentaDestinoId())
                .orElseThrow(() -> new CuentaNoEncontradaException(comando.cuentaDestinoId()));

        if (!origen.getMoneda().equals(destino.getMoneda())) {
            throw new TransferenciaInvalidaException(
                    "Las cuentas tienen distinta moneda: %s vs %s".formatted(origen.getMoneda(), destino.getMoneda()));
        }

        // 4) Regla de negocio central: si no hay saldo, lanza Cuenta.SaldoInsuficienteException
        //    y nada de lo anterior se guarda (rollback automático por @Transactional)
        origen.debitar(comando.monto());
        destino.acreditar(comando.monto());

        // 5) Persistir: la transferencia y los dos movimientos del libro contable
        Transferencia transferencia = Transferencia.completada(
                comando.claveIdempotencia(), origen, destino, comando.monto());
        transferenciaRepository.save(transferencia);

        movimientoRepository.save(new Movimiento(origen, transferencia, TipoMovimiento.DEBITO,
                comando.monto(), origen.getSaldo()));
        movimientoRepository.save(new Movimiento(destino, transferencia, TipoMovimiento.CREDITO,
                comando.monto(), destino.getSaldo()));

        return new TransferenciaResultado(
                transferencia.getId(), transferencia.getClaveIdempotencia(),
                transferencia.getEstado(), origen.getSaldo());
    }
}
