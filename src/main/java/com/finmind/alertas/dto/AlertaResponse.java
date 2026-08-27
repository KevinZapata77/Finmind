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
        String rutaSugerida,

        /**
         * RF-051. Que registro concreto tiene que abrir el usuario:
         * CATEGORIA, CUENTA u OBLIGACION. Nulo en los avisos que hablan del
         * mes entero y no de un registro (el ritmo de gasto, por ejemplo).
         *
         * POR QUE EL TIPO NOMBRA EL DESTINO Y NO LA CAUSA
         * El aviso "vas a pasarte en Alimentacion" lo dispara un presupuesto,
         * pero lo que el usuario necesita ver son los movimientos de esa
         * categoria: el presupuesto ya lo sabe, lo que no sabe es en que se
         * fue la plata. Asi que ese aviso referencia la CATEGORIA, no el
         * presupuesto. El campo describe a donde hay que ir, que es lo unico
         * que el cliente puede hacer con el.
         *
         * ANTES DE ESTO
         * `rutaSugerida` apuntaba a un modulo ("/presupuestos"), nunca a un
         * registro. El nombre de la entidad solo existia incrustado en el texto
         * de `mensaje`, asi que resaltar el registro exacto habria obligado al
         * cliente a interpretar una frase en espanol para sacar un id. Un aviso
         * dejaba al usuario en la pantalla correcta y buscando a mano.
         */
        String referenciaTipo,
        Long referenciaId
) {
    /** Aviso sobre el mes en general, sin un registro al que apuntar. */
    public static AlertaResponse de(String tipo, String severidad, String titulo,
                                    String mensaje, BigDecimal monto, String ruta) {
        return new AlertaResponse(tipo, severidad, titulo, mensaje, monto, ruta, null, null);
    }

    /** RF-051. Aviso que apunta a un registro concreto. */
    public static AlertaResponse de(String tipo, String severidad, String titulo,
                                    String mensaje, BigDecimal monto, String ruta,
                                    String referenciaTipo, Long referenciaId) {
        return new AlertaResponse(tipo, severidad, titulo, mensaje, monto, ruta,
                referenciaTipo, referenciaId);
    }
}
