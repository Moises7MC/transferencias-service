package pe.jllalle.transferenciasservice.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferenciaRequest(

        @NotBlank(message = "La clave de idempotencia es obligatoria")
        String claveIdempotencia,

        @NotNull(message = "La cuenta de origen es obligatoria")
        Long cuentaOrigenId,

        @NotNull(message = "La cuenta de destino es obligatoria")
        Long cuentaDestinoId,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal monto
) {
}
