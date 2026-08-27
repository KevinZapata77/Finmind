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

        /**
         * En MENSUAL y QUINCENAL es el dia del mes (1-28, para que exista en
         * cualquier mes). En SEMANAL es el dia de la semana (1 lunes, ..., 7
         * domingo). La anotacion solo puede exigir un rango fijo; el limite
         * real de 1 a 7 en el caso semanal lo revisa el servicio, que ya sabe
         * la periodicidad (DEF-19).
         */
        @NotNull(message = "El dia de pago es obligatorio")
        @Min(value = 1, message = "El dia debe ser al menos 1")
        @Max(value = 28, message = "El dia no puede pasar de 28")
        Short diaPago
) {
}
