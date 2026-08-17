package com.finmind.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * RF-021. Balance del periodo.
 *
 * `lectura` explica el resultado en palabras porque el signo de un numero
 * grande es facil de pasar por alto, y porque el color no puede ser el unico
 * indicador de algo tan importante como gastar mas de lo que se ingresa.
 */
public record BalanceResponse(
        LocalDate desde, LocalDate hasta,
        BigDecimal ingresos, BigDecimal gastos, BigDecimal diferencia,
        BigDecimal porcentajeGastado, String lectura
) {
    public static BalanceResponse de(LocalDate desde, LocalDate hasta,
                                     BigDecimal ingresos, BigDecimal gastos) {
        BigDecimal diferencia = ingresos.subtract(gastos);
        BigDecimal pct = ingresos.signum() == 0
                ? BigDecimal.ZERO.setScale(1)
                : gastos.multiply(new BigDecimal("100"))
                        .divide(ingresos, 1, java.math.RoundingMode.HALF_UP);

        String lectura;
        if (ingresos.signum() == 0 && gastos.signum() == 0) {
            lectura = "No hay movimientos en este periodo.";
        } else if (diferencia.signum() > 0) {
            lectura = "Te quedo un excedente de " + diferencia + " en el periodo.";
        } else if (diferencia.signum() == 0) {
            lectura = "Gastaste exactamente lo que ingresaste.";
        } else {
            lectura = "Gastaste " + diferencia.abs() + " mas de lo que ingresaste.";
        }
        return new BalanceResponse(desde, hasta, ingresos, gastos, diferencia, pct, lectura);
    }
}
