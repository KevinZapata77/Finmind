package com.finmind.cuentas.service;

import com.finmind.movimientos.repository.TransaccionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Saldo actual de una cuenta = saldo inicial + ingresos - gastos.
 *
 * DT-09 CERRADA. Mientras no existia el modulo de movimientos esta clase
 * devolvia cero, y el saldo actual coincidia con el inicial. Ahora suma de
 * verdad. La firma no cambio: por eso el resto del modulo de cuentas no se
 * entero de nada.
 *
 * La suma la hace la base, no Java. Traer todos los movimientos de una cuenta
 * para recorrerlos en memoria funciona con datos de prueba y se cae con un
 * usuario que lleve dos anios registrando gastos.
 */
@Component
public class CalculadoraDeSaldo {

    private final TransaccionRepository movimientos;

    public CalculadoraDeSaldo(TransaccionRepository movimientos) {
        this.movimientos = movimientos;
    }

    public BigDecimal movimientoNetoDe(Long cuentaId) {
        if (cuentaId == null) return BigDecimal.ZERO;
        return movimientos.netoDeLaCuenta(cuentaId);
    }
}
