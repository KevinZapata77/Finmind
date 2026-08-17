package com.finmind.categorias.dto;

import jakarta.validation.constraints.*;

/**
 * Sin el tipo a proposito: si una categoria de GASTO pasara a INGRESO, todos
 * los movimientos ya registrados con ella quedarian contados al reves.
 */
public record EditarCategoriaRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
        String nombre,

        @Size(max = 40, message = "El icono no puede superar 40 caracteres")
        String icono,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe ser un hexadecimal como #0E8368")
        String colorHex
) {
}
