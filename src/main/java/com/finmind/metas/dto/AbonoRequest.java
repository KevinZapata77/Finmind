package com.finmind.metas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** RF-033. */
public record AbonoRequest(

        @NotNull(message = "El monto del abono es obligatorio")
        @DecimalMin(value = "0.01", message = "El abono debe ser mayor que cero")
        @Digits(integer = 13, fraction = 2, message = "El monto admite maximo 2 decimales")
        BigDecimal monto
) {
}
