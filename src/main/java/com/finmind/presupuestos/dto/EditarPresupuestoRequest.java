package com.finmind.presupuestos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Solo el limite. Cambiar la categoria o el periodo no seria editar este
 * presupuesto sino crear otro distinto, y dejaria el consumo ya calculado
 * apuntando a un periodo que no le corresponde.
 */
public record EditarPresupuestoRequest(

        @NotNull(message = "El monto limite es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto limite debe ser mayor que cero")
        @Digits(integer = 13, fraction = 2, message = "El monto admite maximo 2 decimales")
        BigDecimal montoLimite
) {
}
