package com.finmind.obligaciones.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** RF-036. El desglose entre interes y capital lo calcula el servidor (RN-018). */
public record RegistrarPagoRequest(

        @NotNull(message = "El monto del pago es obligatorio")
        @DecimalMin(value = "0.01", message = "El pago debe ser mayor que cero")
        @Digits(integer = 13, fraction = 2, message = "El monto admite maximo 2 decimales")
        BigDecimal monto,

        @NotNull(message = "La fecha del pago es obligatoria")
        @PastOrPresent(message = "La fecha del pago no puede estar en el futuro")
        LocalDate fecha,

        @Size(max = 255, message = "La descripcion no puede superar 255 caracteres")
        String descripcion
) {
}
