package com.finmind.metas.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** RF-032. */
public record MetaRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @NotNull(message = "El monto objetivo es obligatorio")
        @DecimalMin(value = "0.01", message = "El objetivo debe ser mayor que cero")
        @Digits(integer = 13, fraction = 2, message = "El monto admite maximo 2 decimales")
        BigDecimal montoObjetivo,

        // Opcional: hay metas sin plazo. Si se pone, tiene que estar adelante.
        @Future(message = "La fecha limite debe estar en el futuro")
        LocalDate fechaLimite
) {
}
