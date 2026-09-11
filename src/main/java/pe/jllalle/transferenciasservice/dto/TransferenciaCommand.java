package pe.jllalle.transferenciasservice.dto;

import java.math.BigDecimal;

/** Comando de entrada al servicio: qué se pide, sin depender de JPA ni del transporte HTTP. */
public record TransferenciaCommand(
        String claveIdempotencia,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal monto
) {
}
