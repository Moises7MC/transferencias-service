package pe.jllalle.transferenciasservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.jllalle.transferenciasservice.domain.Cuenta;
import pe.jllalle.transferenciasservice.domain.EstadoTransferencia;
import pe.jllalle.transferenciasservice.domain.Transferencia;
import pe.jllalle.transferenciasservice.dto.TransferenciaCommand;
import pe.jllalle.transferenciasservice.dto.TransferenciaResultado;
import pe.jllalle.transferenciasservice.repository.CuentaRepository;
import pe.jllalle.transferenciasservice.repository.MovimientoRepository;
import pe.jllalle.transferenciasservice.repository.TransferenciaRepository;
import pe.jllalle.transferenciasservice.service.exception.CuentaNoEncontradaException;
import pe.jllalle.transferenciasservice.service.exception.TransferenciaInvalidaException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD: estas pruebas se escribieron ANTES que TransferenciaService.
 * Ciclo rojo-verde-refactor: primero fallan porque la clase no existe (rojo),
 * luego se implementa lo mínimo para que pasen (verde).
 * No usan Spring ni base de datos real: son pruebas unitarias puras con Mockito,
 * por eso corren en milisegundos y son las que primero ejecuta el pipeline de CI.
 */
@ExtendWith(MockitoExtension.class)
class TransferenciaServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private TransferenciaRepository transferenciaRepository;
    @Mock
    private MovimientoRepository movimientoRepository;

    @InjectMocks
    private TransferenciaService service;

    private Cuenta origen;
    private Cuenta destino;

    @BeforeEach
    void setUp() {
        origen = new Cuenta("001-0001", "James Llalle", new BigDecimal("500.00"), "PEN");
        destino = new Cuenta("001-0002", "Ana Torres", new BigDecimal("100.00"), "PEN");
    }

    @Test
    void transfiereYDebitaCreditaLosMontosCorrectos() {
        var comando = new TransferenciaCommand("clave-1", 1L, 2L, new BigDecimal("150.00"));

        when(transferenciaRepository.findByClaveIdempotencia("clave-1")).thenReturn(Optional.empty());
        when(cuentaRepository.findWithLockById(1L)).thenReturn(Optional.of(origen));
        when(cuentaRepository.findWithLockById(2L)).thenReturn(Optional.of(destino));
        when(transferenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransferenciaResultado resultado = service.transferir(comando);

        assertThat(resultado.estado()).isEqualTo(EstadoTransferencia.COMPLETADA);
        assertThat(origen.getSaldo()).isEqualByComparingTo("350.00");
        assertThat(destino.getSaldo()).isEqualByComparingTo("250.00");
        verify(movimientoRepository, times(2)).save(any());
    }

    @Test
    void esIdempotente_siLaClaveYaFueProcesada_noVuelveADebitar() {
        var comando = new TransferenciaCommand("clave-repetida", 1L, 2L, new BigDecimal("150.00"));
        var transferenciaPrevia = Transferencia.completada("clave-repetida", origen, destino, new BigDecimal("150.00"));

        when(transferenciaRepository.findByClaveIdempotencia("clave-repetida"))
                .thenReturn(Optional.of(transferenciaPrevia));

        TransferenciaResultado resultado = service.transferir(comando);

        assertThat(resultado.estado()).isEqualTo(EstadoTransferencia.COMPLETADA);
        verify(cuentaRepository, never()).findWithLockById(any());
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void rechazaTransferenciaEntreLaMismaCuenta() {
        var comando = new TransferenciaCommand("clave-2", 1L, 1L, new BigDecimal("50.00"));
        when(transferenciaRepository.findByClaveIdempotencia("clave-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transferir(comando))
                .isInstanceOf(TransferenciaInvalidaException.class)
                .hasMessageContaining("misma cuenta");

        verifyNoInteractions(cuentaRepository);
    }

    @Test
    void rechazaMontoMenorOIgualACero() {
        var comando = new TransferenciaCommand("clave-3", 1L, 2L, BigDecimal.ZERO);
        when(transferenciaRepository.findByClaveIdempotencia("clave-3")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transferir(comando))
                .isInstanceOf(TransferenciaInvalidaException.class)
                .hasMessageContaining("mayor a cero");
    }

    @Test
    void rechazaSiLaCuentaOrigenNoExiste() {
        var comando = new TransferenciaCommand("clave-4", 1L, 2L, new BigDecimal("10.00"));
        when(transferenciaRepository.findByClaveIdempotencia("clave-4")).thenReturn(Optional.empty());
        when(cuentaRepository.findWithLockById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transferir(comando))
                .isInstanceOf(CuentaNoEncontradaException.class);
    }

    @Test
    void rechazaSiElSaldoEsInsuficiente() {
        var comando = new TransferenciaCommand("clave-5", 1L, 2L, new BigDecimal("999999.00"));
        when(transferenciaRepository.findByClaveIdempotencia("clave-5")).thenReturn(Optional.empty());
        when(cuentaRepository.findWithLockById(1L)).thenReturn(Optional.of(origen));
        when(cuentaRepository.findWithLockById(2L)).thenReturn(Optional.of(destino));

        assertThatThrownBy(() -> service.transferir(comando))
                .isInstanceOf(Cuenta.SaldoInsuficienteException.class);

        // el saldo no debe haber cambiado: si algo falla, no debe quedar a medias
        assertThat(origen.getSaldo()).isEqualByComparingTo("500.00");
        verify(transferenciaRepository, never()).save(any());
    }

    @Test
    void rechazaSiLasMonedasNoCoinciden() {
        var destinoOtraMoneda = new Cuenta("001-0009", "Cliente USD", new BigDecimal("100.00"), "USD");
        var comando = new TransferenciaCommand("clave-6", 1L, 2L, new BigDecimal("10.00"));

        when(transferenciaRepository.findByClaveIdempotencia("clave-6")).thenReturn(Optional.empty());
        when(cuentaRepository.findWithLockById(1L)).thenReturn(Optional.of(origen));
        when(cuentaRepository.findWithLockById(2L)).thenReturn(Optional.of(destinoOtraMoneda));

        assertThatThrownBy(() -> service.transferir(comando))
                .isInstanceOf(TransferenciaInvalidaException.class)
                .hasMessageContaining("moneda");
    }
}
