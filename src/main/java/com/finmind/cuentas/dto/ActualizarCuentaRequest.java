package com.finmind.cuentas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Datos editables de una cuenta (RF-008).
 *
 * No incluye saldo inicial ni moneda a proposito: cambiarlos recalcularia
 * hacia atras todos los saldos y los movimientos ya registrados dejarian de
 * cuadrar con el historial.
 *
 * El cupo si es editable: el banco lo sube o lo baja con el tiempo y cambiarlo
 * no altera ningun movimiento pasado, solo lo que queda por gastar.
 */
public record ActualizarCuentaRequest(

        @NotBlank(message = "El nombre de la cuenta es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @NotBlank(message = "El tipo de cuenta es obligatorio")
        String tipo,

        /** Cupo total, solo para tarjetas. Enviar null lo deja sin registrar. */
        @DecimalMin(value = "0.01", message = "El cupo debe ser mayor que cero")
        @Digits(integer = 13, fraction = 2, message = "El cupo admite maximo 2 decimales")
        BigDecimal cupo
) {
}
