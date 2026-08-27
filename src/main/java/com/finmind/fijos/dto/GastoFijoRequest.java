package com.finmind.fijos.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** RF-046. Alta y edicion de un compromiso recurrente. */
public record GastoFijoRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @NotNull(message = "La categoria es obligatoria")
        Long categoriaId,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
        @Digits(integer = 13, fraction = 2, message = "El monto admite maximo 2 decimales")
        BigDecimal monto,

        @NotBlank(message = "Indica cada cuanto se paga")
        String periodicidad,

        // Hasta 28 para que el dia exista en cualquier mes, febrero incluido.
        @NotNull(message = "El dia de pago es obligatorio")
        @Min(value = 1, message = "El dia va de 1 a 28")
        @Max(value = 28, message = "El dia va de 1 a 28")
        Short diaPago
) {
}
