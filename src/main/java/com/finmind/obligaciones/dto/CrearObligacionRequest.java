package com.finmind.obligaciones.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** RF-035. */
public record CrearObligacionRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @NotBlank(message = "El acreedor es obligatorio")
        @Size(max = 80, message = "El acreedor no puede superar 80 caracteres")
        String acreedor,

        @NotBlank(message = "El tipo de obligacion es obligatorio")
        String tipo,

        @NotNull(message = "El monto original es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto original debe ser mayor que cero")
        @Digits(integer = 13, fraction = 2, message = "El monto admite maximo 2 decimales")
        BigDecimal montoOriginal,

        // Cero es valido: existen deudas sin interes, como la de un familiar.
        // El techo de 200 atrapa el error de escribir 2400 en lugar de 24.
        @DecimalMin(value = "0.0", message = "La tasa no puede ser negativa")
        @DecimalMax(value = "200.0", message = "Revisa la tasa: 24.5 significa 24,5% anual")
        @Digits(integer = 3, fraction = 4, message = "La tasa admite maximo 4 decimales")
        BigDecimal tasaAnual,

        @NotNull(message = "La cuota mensual es obligatoria")
        @DecimalMin(value = "0.01", message = "La cuota debe ser mayor que cero")
        @Digits(integer = 13, fraction = 2, message = "La cuota admite maximo 2 decimales")
        BigDecimal cuotaMensual,

        // Hasta 28 para que la fecha exista en todos los meses, febrero incluido.
        @NotNull(message = "El dia de pago es obligatorio")
        @Min(value = 1, message = "El dia de pago va de 1 a 28")
        @Max(value = 28, message = "El dia de pago va de 1 a 28")
        Short diaPago,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio
) {
    public BigDecimal tasaOCero() {
        return tasaAnual == null ? BigDecimal.ZERO : tasaAnual;
    }
}
