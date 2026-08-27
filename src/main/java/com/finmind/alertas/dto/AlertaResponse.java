package com.finmind.alertas.dto;

import java.math.BigDecimal;

/**
 * RF-047. Un aviso que la aplicacion le da al usuario sobre su mes.
 *
 * NO SE GUARDA EN NINGUNA TABLA
 * Una alerta es un hecho derivado de los movimientos, los presupuestos y los
 * compromisos: se calcula cada vez que se pide. Guardarla crearia el mismo
 * problema que se evito con los saldos — dos versiones de la verdad — y una
 * alerta guardada seguiria diciendo "no te alcanza" despues de que el usuario
 * cobrara.
 */
public record AlertaResponse(
        /** Que clase de aviso es. Permite al cliente elegir icono y orden. */
        String tipo,
        /** ALTA, MEDIA o BAJA. La severidad decide el color y la posicion. */
        String severidad,
        String titulo,
        /** Explicacion en palabras, con las cifras dentro. */
        String mensaje,
        /** Monto al que se refiere, si aplica. Nulo cuando no hay uno solo. */
        BigDecimal monto,
        /**
         * A donde deberia ir el usuario para actuar. El aviso sin salida
         * frustra: si le dices que no le alcanza, dile tambien donde mirarlo.
         */
        String rutaSugerida
) {
    public static AlertaResponse de(String tipo, String severidad, String titulo,
                                    String mensaje, BigDecimal monto, String ruta) {
        return new AlertaResponse(tipo, severidad, titulo, mensaje, monto, ruta);
    }
}
