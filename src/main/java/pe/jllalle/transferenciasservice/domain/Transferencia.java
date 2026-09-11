package pe.jllalle.transferenciasservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transferencias")
@Getter
@NoArgsConstructor
public class Transferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clave_idempotencia", nullable = false, unique = true)
    private String claveIdempotencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_origen_id")
    private Cuenta cuentaOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_destino_id")
    private Cuenta cuentaDestino;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false)
    private String moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTransferencia estado;

    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    public static Transferencia completada(String claveIdempotencia, Cuenta origen, Cuenta destino, BigDecimal monto) {
        Transferencia t = new Transferencia();
        t.claveIdempotencia = claveIdempotencia;
        t.cuentaOrigen = origen;
        t.cuentaDestino = destino;
        t.monto = monto;
        t.moneda = origen.getMoneda();
        t.estado = EstadoTransferencia.COMPLETADA;
        t.creadoEn = LocalDateTime.now();
        return t;
    }
}
