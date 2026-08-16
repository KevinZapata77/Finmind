package com.finmind.cuentas.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Saldo actual de una cuenta = saldo inicial + ingresos - gastos.
 *
 * LIMITACION CONOCIDA (DT-09): el modulo de movimientos todavia no existe, asi
 * que hoy no hay nada que sumar y el neto es cero. Es decir: el saldo actual
 * que se devuelve es correcto -- una cuenta sin movimientos tiene exactamente
 * su saldo inicial -- pero estara incompleto en cuanto se registren
 * movimientos, si no se completa esta clase primero.
 *
 * Existe como clase aparte, y no como una linea suelta dentro del servicio,
 * para que al construir el modulo de movimientos el cambio sea evidente y
 * ocurra en un solo lugar. La firma ya es la definitiva.
 */
@Component
public class CalculadoraDeSaldo {

    public BigDecimal movimientoNetoDe(Long cuentaId) {
        return BigDecimal.ZERO;
    }
}
