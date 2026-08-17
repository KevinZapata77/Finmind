package com.finmind.obligaciones.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Datos editables. No incluye monto original, tasa ni fecha de inicio: cambiarlos
 * invalidaria la descomposicion de todos los pagos ya registrados.
 */
public record ActualizarObligacionRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @NotBlank(message = "El acreedor es obligatorio")
        @Size(max = 80, message = "El acreedor no puede superar 80 caracteres")
        String acreedor,

        @NotNull(message = "La cuota mensual es obligatoria")
        @DecimalMin(value = "0.01", message = "La cuota debe ser mayor que cero")
        @Digits(integer = 13, fraction = 2, message = "La cuota admite maximo 2 decimales")
        BigDecimal cuotaMensual,

        @NotNull(message = "El dia de pago es obligatorio")
        @Min(value = 1, message = "El dia de pago va de 1 a 28")
        @Max(value = 28, message = "El dia de pago va de 1 a 28")
        Short diaPago
) {
}
