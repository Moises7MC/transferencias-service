package pe.jllalle.transferenciasservice.web;

import pe.jllalle.transferenciasservice.domain.Cuenta;

import java.math.BigDecimal;

public record CuentaResponse(Long id, String numeroCuenta, String titular, BigDecimal saldo, String moneda) {

    public static CuentaResponse desde(Cuenta c) {
        return new CuentaResponse(c.getId(), c.getNumeroCuenta(), c.getTitular(), c.getSaldo(), c.getMoneda());
    }
}
