package com.finmind.cuentas.dto;

import com.finmind.cuentas.entity.Cuenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Vista publica de una cuenta. Nunca expone el usuario dueño. */
public record CuentaResponse(
        Long id,
        String nombre,
        String tipo,
        BigDecimal saldoInicial,
        BigDecimal saldoActual,
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
                cuenta.getMoneda(),
                cuenta.getActiva(),
                cuenta.getFechaCreacion());
    }
}
