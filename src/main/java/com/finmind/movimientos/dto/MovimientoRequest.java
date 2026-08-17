package com.finmind.movimientos.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * RF-012 y RF-013.
 *
 * No lleva `tipo`: lo determina la categoria (RN-002). Si viniera aparte se
 * podria registrar un INGRESO con categoria Alimentacion, y el balance por
 * categoria dejaria de cuadrar con el balance general.
 */
public record MovimientoRequest(

        @NotNull(message = "La cuenta es obligatoria")
        Long cuentaId,

        @NotNull(message = "La categoria es obligatoria")
        Long categoriaId,

        // RN-001: el signo lo pone el tipo, nunca el monto.
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
        @Digits(integer = 13, fraction = 2, message = "El monto admite maximo 2 decimales")
        BigDecimal monto,

        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "No se pueden registrar movimientos con fecha futura")
        LocalDate fecha,

        @Size(max = 255, message = "La descripcion no puede superar 255 caracteres")
        String descripcion
) {
}
