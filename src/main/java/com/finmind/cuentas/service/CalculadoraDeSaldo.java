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

    /**
     * Movimiento neto de la cuenta: lo que entro menos lo que salio.
     *
     * Son dos consultas porque una transferencia se guarda en UNA sola fila, y
     * esa fila pertenece a la cuenta de origen. Desde la cuenta de destino solo
     * se ve buscando por cuenta_destino_id, asi que hay que sumarla aparte.
     *
     *   ingresos - gastos - transferencias que salieron     (primera consulta)
     *   + transferencias que entraron                       (segunda)
     */
    public BigDecimal movimientoNetoDe(Long cuentaId) {
        if (cuentaId == null) return BigDecimal.ZERO;
        return movimientos.salidasYEntradasPropias(cuentaId)
                .add(movimientos.recibidoPorTransferencia(cuentaId));
    }

    /**
     * RF-044. Total abonado a la cuenta, o sea la suma de sus ingresos.
     *
     * En una tarjeta de credito eso es lo que le has pagado. Se consulta
     * aparte y no dentro del neto porque el neto ya combina ingresos y gastos
     * en un solo numero, y aqui hace falta el ingreso por separado.
     */
    public BigDecimal totalAbonadoA(Long cuentaId) {
        if (cuentaId == null) return BigDecimal.ZERO;
        return movimientos.totalAbonadoALaCuenta(cuentaId);
    }
}
