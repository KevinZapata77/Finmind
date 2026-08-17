package com.finmind.presupuestos.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** RF-017. */
public record PresupuestoRequest(

        @NotNull(message = "La categoria es obligatoria")
        Long categoriaId,

        // RN-008
        @NotNull(message = "El monto limite es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto limite debe ser mayor que cero")
        @Digits(integer = 13, fraction = 2, message = "El monto admite maximo 2 decimales")
        BigDecimal montoLimite,

        // RN-007
        @NotNull(message = "El anio es obligatorio")
        @Min(value = 2000, message = "El anio va de 2000 a 2100")
        @Max(value = 2100, message = "El anio va de 2000 a 2100")
        Short anio,

        @NotNull(message = "El mes es obligatorio")
        @Min(value = 1, message = "El mes va de 1 a 12")
        @Max(value = 12, message = "El mes va de 1 a 12")
        Short mes,

        String periodo
) {
    public String periodoOMensual() {
        return periodo == null || periodo.isBlank() ? "MENSUAL" : periodo.trim().toUpperCase();
    }
}
