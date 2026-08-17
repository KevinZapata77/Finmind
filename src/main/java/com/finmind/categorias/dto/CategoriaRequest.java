package com.finmind.categorias.dto;

import jakarta.validation.constraints.*;

/** RF-009. El tipo no se edita despues: cambiarlo dejaria movimientos mal clasificados. */
public record CategoriaRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
        String nombre,

        @NotBlank(message = "El tipo es obligatorio")
        String tipo,

        @Size(max = 40, message = "El icono no puede superar 40 caracteres")
        String icono,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe ser un hexadecimal como #0E8368")
        String colorHex
) {
}
