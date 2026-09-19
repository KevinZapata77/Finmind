package com.finmind.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "El correo es obligatorio")
        String correo,

        @NotBlank(message = "La contrasena es obligatoria")
        String contrasena,

        /**
         * Token del CAPTCHA. NO lleva @NotBlank a proposito.
         *
         * Quien decide si hace falta es el servidor, no la anotacion: con
         * finmind.captcha.habilitado en false —desarrollo y el perfil de
         * pruebas— ServicioCaptcha sale temprano y este campo se ignora. Si
         * fuera obligatorio aqui, las 222 pruebas automatizadas y la consola de
         * Swagger dejarian de poder iniciar sesion, y el control se volveria un
         * estorbo en vez de una defensa.
         *
         * Con el CAPTCHA encendido, un token vacio se rechaza igual — pero lo
         * rechaza ServicioCaptcha, que es el unico que sabe si esta encendido.
         */
        String captchaToken
) {
}
