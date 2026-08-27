package com.finmind.reportes.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * RF-050. Ingresos y gastos mes por mes, y la comparacion con el mes anterior.
 *
 * POR QUE HACIA FALTA
 * Todos los demas reportes reciben un anio y un mes sueltos, asi que la
 * aplicacion solo sabia hablar del presente: podia decir "gastaste 1.200.000
 * en agosto", pero no "gastaste 300.000 mas que en julio". Un mes aislado es
 * una foto; varios meses seguidos son una tendencia, y la tendencia es lo que
 * permite ver que el gasto se esta subiendo ANTES de que se vuelva un problema.
 *
 * Para armar esta serie el frontend habria tenido que llamar al balance una vez
 * por mes. Seis llamadas encadenadas contra una base que ademas se duerme
 * (Neon) es lento y se ve lento.
 */
public record HistoricoResponse(

        /** De mas antiguo a mas reciente: es el orden en que se dibuja. */
        List<Mes> meses,

        /**
         * La comparacion contra el mes anterior. Nula cuando no hay con que
         * comparar (RN-032).
         */
        Comparacion comparacion
) {
    /**
     * Un mes de la serie. Lleva `anio` y `mes` por separado y no una fecha
     * porque quien lo dibuja necesita el numero del mes para rotular el eje,
     * no una fecha que tendria que volver a partir.
     */
    public record Mes(int anio, int mes, String nombre,
                      BigDecimal ingresos, BigDecimal gastos, BigDecimal diferencia) {
    }

    /**
     * RN-032. Comparacion del mes en curso contra el anterior, MIDIENDO EL
     * MISMO NUMERO DE DIAS EN LOS DOS.
     *
     * POR QUE NO SE COMPARAN LOS TOTALES DE LOS DOS MESES
     * Seria la version obvia y estaria mal. El mes en curso va por la mitad y
     * el anterior esta completo, asi que el dia 5 la comparacion diria siempre
     * que el usuario esta gastando muchisimo menos, y volveria a decirlo el mes
     * siguiente, y el siguiente. Un numero que siempre da buenas noticias no es
     * una buena noticia: es un numero roto, y en una aplicacion de dinero un
     * numero roto que tranquiliza es peor que no mostrar nada.
     *
     * Se compara entonces del dia 1 al dia de hoy contra del dia 1 al mismo dia
     * del mes anterior. Eso si es comparable, y es lo que permite decir "vas
     * gastando 80.000 mas que a esta altura del mes pasado".
     */
    public record Comparacion(

            /** Hasta que dia se midieron los dos meses. */
            int diaDeCorte,

            BigDecimal gastoEsteMes,
            BigDecimal gastoMesAnterior,

            /** Positiva si se esta gastando mas que el mes pasado. */
            BigDecimal variacion,

            /**
             * La misma variacion en porcentaje. Nula cuando el mes anterior
             * cerro en cero ese tramo: una cifra dividida por cero no es
             * "infinito por ciento", es una pregunta mal planteada. La cifra
             * absoluta si se puede mostrar.
             */
            BigDecimal variacionPorcentaje
    ) {
    }
}
