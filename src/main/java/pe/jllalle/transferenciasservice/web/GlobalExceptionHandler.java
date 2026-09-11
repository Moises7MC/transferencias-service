package pe.jllalle.transferenciasservice.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.jllalle.transferenciasservice.domain.Cuenta;
import pe.jllalle.transferenciasservice.service.exception.CuentaNoEncontradaException;
import pe.jllalle.transferenciasservice.service.exception.TransferenciaInvalidaException;

/**
 * Centraliza el mapeo de excepciones a respuestas HTTP usando ProblemDetail (RFC 7807).
 * Principio OWASP aplicado: nunca se devuelve el stack trace ni el mensaje interno
 * de una excepción no controlada al cliente; solo se loguea del lado del servidor.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ProblemDetail manejarCuentaNoEncontrada(CuentaNoEncontradaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TransferenciaInvalidaException.class)
    public ProblemDetail manejarTransferenciaInvalida(TransferenciaInvalidaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(Cuenta.SaldoInsuficienteException.class)
    public ProblemDetail manejarSaldoInsuficiente(Cuenta.SaldoInsuficienteException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail manejarValidacion(MethodArgumentNotValidException ex) {
        ProblemDetail detalle = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos");
        ex.getBindingResult().getFieldErrors().forEach(error ->
                detalle.setProperty(((FieldError) error).getField(), error.getDefaultMessage()));
        return detalle;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarErrorInesperado(Exception ex) {
        log.error("Error no controlado", ex); // el detalle queda en el log del servidor, no en la respuesta
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado");
    }
}
