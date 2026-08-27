package com.finmind.cuentas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * RF-045. Abono a una tarjeta de credito.
 *
 * La cuenta de origen es obligatoria a proposito. Sin ella el dinero apareceria
 * de la nada: la deuda bajaria pero no se descontaria de ningun lado, y el
 * usuario terminaria con mas plata de la que tiene.
 */
public record AbonoTarjetaRequest(

        @NotNull(message = "Indica de que cuenta sale el dinero")
        Long cuentaOrigenId,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
        @Digits(integer = 13, fraction = 2, message = "El monto admite maximo 2 decimales")
        BigDecimal monto,

        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "No se pueden registrar abonos con fecha futura")
        LocalDate fecha,

        @Size(max = 255, message = "La descripcion no puede superar 255 caracteres")
        String descripcion
) {
}
