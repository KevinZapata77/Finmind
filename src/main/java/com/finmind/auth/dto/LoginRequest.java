package com.finmind.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "El correo es obligatorio")
        String correo,

        @NotBlank(message = "La contrasena es obligatoria")
        String contrasena
) {
}
