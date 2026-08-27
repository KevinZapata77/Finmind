package com.finmind.obligaciones.dto;

import java.math.BigDecimal;

/**
 * RF-038. Lo que el usuario tiene, lo que debe y la diferencia.
 *
 * La deuda va desglosada en dos: los prestamos del modulo de obligaciones y lo
 * gastado en tarjetas de credito. Antes solo se contaban los prestamos, asi que
 * una tarjeta registrada como cuenta no aparecia por ningun lado (DEF-14).
 * Mostrarlas separadas evita la pregunta obvia de "de donde sale ese numero".
 *
 * El texto de `lectura` existe para no depender del color: un patrimonio
 * negativo tiene que entenderse tambien leyendo (RNF-008).
 */
public record PatrimonioResponse(
        BigDecimal activos,
        /** Prestamos y creditos del modulo de obligaciones. */
        BigDecimal obligaciones,
        /** Lo gastado y aun no pagado en tarjetas de credito. */
        BigDecimal deudaEnTarjetas,
        /** obligaciones + deudaEnTarjetas. Es lo que se resta de los activos. */
        BigDecimal deudaTotal,
        BigDecimal patrimonioNeto,
        String lectura
) {
    public static PatrimonioResponse de(BigDecimal activos,
                                        BigDecimal obligaciones,
                                        BigDecimal deudaEnTarjetas) {
        BigDecimal tarjetas = deudaEnTarjetas == null ? BigDecimal.ZERO : deudaEnTarjetas;
        BigDecimal deudaTotal = obligaciones.add(tarjetas);
        BigDecimal neto = activos.subtract(deudaTotal);

        String lectura = switch (neto.signum()) {
            case 1 -> "Tus activos superan tus obligaciones.";
            case 0 -> "Tus activos igualan exactamente tus obligaciones.";
            default -> "Debes mas de lo que tienes. El patrimonio neto es negativo.";
        };
        return new PatrimonioResponse(activos, obligaciones, tarjetas, deudaTotal, neto, lectura);
    }
}
