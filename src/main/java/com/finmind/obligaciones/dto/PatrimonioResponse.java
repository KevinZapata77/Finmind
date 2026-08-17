package com.finmind.obligaciones.dto;

import java.math.BigDecimal;

/**
 * RF-038. Lo que el usuario tiene, lo que debe y la diferencia.
 *
 * El texto de `lectura` existe para no depender del color: un patrimonio
 * negativo tiene que entenderse tambien leyendo (RNF de accesibilidad).
 */
public record PatrimonioResponse(
        BigDecimal activos,
        BigDecimal obligaciones,
        BigDecimal patrimonioNeto,
        String lectura
) {
    public static PatrimonioResponse de(BigDecimal activos, BigDecimal obligaciones) {
        BigDecimal neto = activos.subtract(obligaciones);
        String lectura = switch (neto.signum()) {
            case 1 -> "Tus activos superan tus obligaciones.";
            case 0 -> "Tus activos igualan exactamente tus obligaciones.";
            default -> "Debes mas de lo que tienes. El patrimonio neto es negativo.";
        };
        return new PatrimonioResponse(activos, obligaciones, neto, lectura);
    }
}
