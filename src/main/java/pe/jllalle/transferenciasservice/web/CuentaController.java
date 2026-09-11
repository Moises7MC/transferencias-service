package pe.jllalle.transferenciasservice.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.jllalle.transferenciasservice.repository.CuentaRepository;
import pe.jllalle.transferenciasservice.service.exception.CuentaNoEncontradaException;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
@Tag(name = "Cuentas", description = "Consulta de cuentas y saldos")
public class CuentaController {

    private final CuentaRepository cuentaRepository;

    @GetMapping
    public List<CuentaResponse> listar() {
        return cuentaRepository.findAll().stream().map(CuentaResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuentaResponse> obtener(@PathVariable Long id) {
        var cuenta = cuentaRepository.findById(id)
                .orElseThrow(() -> new CuentaNoEncontradaException(id));
        return ResponseEntity.ok(CuentaResponse.desde(cuenta));
    }
}
