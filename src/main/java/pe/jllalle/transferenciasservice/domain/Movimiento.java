package pe.jllalle.transferenciasservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos")
@Getter
@NoArgsConstructor
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id")
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transferencia_id")
    private Transferencia transferencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(name = "saldo_posterior", nullable = false)
    private BigDecimal saldoPosterior;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    public Movimiento(Cuenta cuenta, Transferencia transferencia, TipoMovimiento tipo,
                       BigDecimal monto, BigDecimal saldoPosterior) {
        this.cuenta = cuenta;
        this.transferencia = transferencia;
        this.tipo = tipo;
        this.monto = monto;
        this.saldoPosterior = saldoPosterior;
        this.creadoEn = LocalDateTime.now();
    }
}
