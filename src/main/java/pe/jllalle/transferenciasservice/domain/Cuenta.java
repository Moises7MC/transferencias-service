package pe.jllalle.transferenciasservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "cuentas")
@Getter
@NoArgsConstructor // requerido por JPA
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_cuenta", nullable = false, unique = true)
    private String numeroCuenta;

    @Column(nullable = false)
    private String titular;

    @Column(nullable = false)
    private BigDecimal saldo;

    @Column(nullable = false)
    private String moneda;

    public Cuenta(String numeroCuenta, String titular, BigDecimal saldo, String moneda) {
        this.numeroCuenta = numeroCuenta;
        this.titular = titular;
        this.saldo = saldo;
        this.moneda = moneda;
    }

    /** Regla de negocio: no se puede debitar más de lo que hay en la cuenta. */
    public void debitar(BigDecimal monto) {
        if (saldo.compareTo(monto) < 0) {
            throw new SaldoInsuficienteException(numeroCuenta, saldo, monto);
        }
        this.saldo = this.saldo.subtract(monto);
    }

    public void acreditar(BigDecimal monto) {
        this.saldo = this.saldo.add(monto);
    }

    /** Excepción específica del dominio: vive junto a la regla que protege. */
    public static class SaldoInsuficienteException extends RuntimeException {
        public SaldoInsuficienteException(String numeroCuenta, BigDecimal saldoActual, BigDecimal montoSolicitado) {
            super("Saldo insuficiente en la cuenta %s: saldo %s, solicitado %s"
                    .formatted(numeroCuenta, saldoActual, montoSolicitado));
        }
    }
}
