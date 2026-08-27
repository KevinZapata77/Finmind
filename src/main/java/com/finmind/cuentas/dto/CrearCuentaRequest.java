package com.finmind.cuentas.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Datos para crear una cuenta (RF-006). La validacion la aplica el servidor. */
public record CrearCuentaRequest(

        @NotBlank(message = "El nombre de la cuenta es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @NotBlank(message = "El tipo de cuenta es obligatorio")
        String tipo,

        // RN-010: 13 enteros y 2 decimales es lo que admite NUMERIC(15,2).
        // Si no se valida aqui, la base rechaza el insert con un error feo.
        @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
        @Digits(integer = 13, fraction = 2, message = "El saldo admite maximo 2 decimales")
        BigDecimal saldoInicial,

        @Pattern(regexp = "^[A-Z]{3}$", message = "La moneda debe ser un codigo de 3 letras, como COP")
        String moneda,

        /**
         * Cupo total, solo para tarjetas de credito (RF-043). Opcional: una
         * tarjeta se puede registrar sin conocerlo todavia.
         */
        @DecimalMin(value = "0.01", message = "El cupo debe ser mayor que cero")
        @Digits(integer = 13, fraction = 2, message = "El cupo admite maximo 2 decimales")
        BigDecimal cupo
) {
    /** Valores por defecto: el contrato permite omitir saldo y moneda. */
    public BigDecimal saldoInicialOCero() {
        return saldoInicial == null ? BigDecimal.ZERO : saldoInicial;
    }

    public String monedaOPorDefecto() {
        return moneda == null || moneda.isBlank() ? "COP" : moneda;
    }
}
