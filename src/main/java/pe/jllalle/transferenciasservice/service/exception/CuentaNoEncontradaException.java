package pe.jllalle.transferenciasservice.service.exception;

public class CuentaNoEncontradaException extends RuntimeException {
    public CuentaNoEncontradaException(Long id) {
        super("No existe la cuenta con id " + id);
    }
}
