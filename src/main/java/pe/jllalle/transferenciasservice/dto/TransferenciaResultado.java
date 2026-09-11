package pe.jllalle.transferenciasservice.dto;

import pe.jllalle.transferenciasservice.domain.EstadoTransferencia;

import java.math.BigDecimal;

public record TransferenciaResultado(
        Long id,
        String claveIdempotencia,
        EstadoTransferencia estado,
        BigDecimal saldoOrigenResultante
) {
}
