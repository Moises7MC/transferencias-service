package pe.jllalle.transferenciasservice.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.jllalle.transferenciasservice.dto.TransferenciaCommand;
import pe.jllalle.transferenciasservice.dto.TransferenciaResultado;
import pe.jllalle.transferenciasservice.service.TransferenciaService;

@RestController
@RequestMapping("/api/transferencias")
@RequiredArgsConstructor
@Tag(name = "Transferencias", description = "Transferencias entre cuentas")
public class TransferenciaController {

    private final TransferenciaService transferenciaService;

    @PostMapping
    @Operation(summary = "Registra una transferencia entre dos cuentas", description =
            "Idempotente: reenviar la misma claveIdempotencia devuelve el mismo resultado sin duplicar el movimiento.")
    public ResponseEntity<TransferenciaResultado> transferir(@Valid @RequestBody TransferenciaRequest request) {
        TransferenciaResultado resultado = transferenciaService.transferir(new TransferenciaCommand(
                request.claveIdempotencia(), request.cuentaOrigenId(), request.cuentaDestinoId(), request.monto()));
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }
}
