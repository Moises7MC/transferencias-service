package pe.jllalle.transferenciasservice.service.exception;

public class TransferenciaInvalidaException extends RuntimeException {
    public TransferenciaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
