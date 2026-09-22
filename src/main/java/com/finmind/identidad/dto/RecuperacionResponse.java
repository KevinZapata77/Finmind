package com.finmind.identidad.dto;

/**
 * Respuesta de "olvide mi contrasena" (RF-027).
 *
 * POR QUE NO ALCANZA CON MensajeResponse
 * Porque el frontend tiene que hacer dos cosas distintas segun el caso, y no
 * puede decidirlo leyendo el texto. Si se enviara el codigo, lleva a la
 * pantalla de escribirlo; si la cuenta entra solo con Google, se queda donde
 * esta y ofrece el boton de Google. Decidir eso comparando cadenas seria atar
 * la navegacion a la redaccion de un mensaje: cambiar una coma romperia el
 * flujo, y sin que ninguna prueba se entere.
 *
 * El campo se llama message para que coincida con MensajeResponse, que es lo
 * que el resto de la API ya devuelve.
 */
public record RecuperacionResponse(String message, boolean usaGoogle) {

    public static RecuperacionResponse enviado() {
        // RN-014: identico exista o no el correo.
        return new RecuperacionResponse(
                "Si ese correo esta registrado, te enviamos un codigo para restablecer la contrasena.",
                false);
    }

    public static RecuperacionResponse soloGoogle() {
        return new RecuperacionResponse(
                "Esa cuenta entra con Google, asi que no tiene contrasena que restablecer. "
                        + "Usa el boton de Google para entrar.",
                true);
    }
}
