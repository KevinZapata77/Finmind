package com.finmind.cuentas.dto;

import com.finmind.cuentas.entity.Cuenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vista publica de una cuenta. Nunca expone el usuario dueño.
 *
 * El significado de saldoActual depende de esPasivo, y por eso el campo va en
 * la respuesta: en una tarjeta de credito el saldo es lo que se DEBE, no lo que
 * se tiene. Sin ese indicador el cliente tendria que deducirlo comparando el
 * tipo contra una lista, y bastaria agregar un tipo nuevo para que se
 * equivocara en silencio.
 */
public record CuentaResponse(
        Long id,
        String nombre,
        String tipo,
        BigDecimal saldoInicial,
        BigDecimal saldoActual,
        /** True si el saldo representa deuda en lugar de dinero disponible. */
        Boolean esPasivo,
        /** Cupo total de la tarjeta. Nulo en los demas tipos. */
        BigDecimal cupo,
        /** Cupo menos deuda. Nulo si no aplica; negativo si se paso del cupo. */
        BigDecimal cupoDisponible,
        String moneda,
        Boolean activa,
        LocalDateTime fechaCreacion
) {
    public static CuentaResponse de(Cuenta cuenta, BigDecimal saldoActual) {
        return new CuentaResponse(
                cuenta.getId(),
                cuenta.getNombre(),
                cuenta.getTipo(),
                cuenta.getSaldoInicial(),
                saldoActual,
                cuenta.esPasivo(),
                cuenta.getCupo(),
                cuenta.cupoDisponibleCon(saldoActual),
                cuenta.getMoneda(),
                cuenta.getActiva(),
                cuenta.getFechaCreacion());
    }
}
