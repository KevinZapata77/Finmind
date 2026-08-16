package com.finmind.identidad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** RF-026 y RF-027: reenviar el codigo o solicitar la recuperacion. */
public record CorreoRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        String correo
) {
}
