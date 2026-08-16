package com.finmind.cuentas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos editables de una cuenta (RF-008).
 *
 * No incluye saldo inicial ni moneda a proposito: cambiarlos recalcularia
 * hacia atras todos los saldos y los movimientos ya registrados dejarian de
 * cuadrar con el historial.
 */
public record ActualizarCuentaRequest(

        @NotBlank(message = "El nombre de la cuenta es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @NotBlank(message = "El tipo de cuenta es obligatorio")
        String tipo
) {
}
