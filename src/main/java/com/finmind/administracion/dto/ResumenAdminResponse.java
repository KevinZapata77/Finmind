package com.finmind.administracion.dto;

/**
 * Conteos de la plataforma. Numeros agregados, nunca datos de una persona.
 *
 * ESA DISTINCION ES EL DISENO, NO UN DETALLE
 * Un administrador de FinMind necesita saber si la plataforma se esta usando.
 * No necesita —y no debe— ver cuanto dinero tiene nadie, ni en que gasta, ni
 * cuales son sus movimientos. Por eso esta pantalla cuenta filas y no las
 * muestra: la diferencia entre una herramienta de operacion y una de vigilancia
 * es exactamente esta.
 *
 * Si algun dia alguien quiere agregar aqui "los gastos totales de la
 * plataforma", conviene releer este parrafo antes.
 *
 * LOS CUATRO PRIMEROS son el estado de las cuentas; LOS CUATRO ULTIMOS, la
 * actividad. Estado responde "cuantos hay"; actividad responde "esto se usa".
 */
public record ResumenAdminResponse(

        /** Todas las cuentas registradas, en cualquier estado. */
        long total,

        /** Activas y con el correo verificado: las que pueden entrar hoy. */
        long activos,

        /** Se registraron pero no escribieron el codigo. No pueden entrar (RN-011). */
        long sinVerificar,

        /** Desactivadas por un administrador. */
        long desactivados,

        /** Cuentas creadas en los ultimos 7 dias. */
        long registrosUltimos7Dias,

        /** Cuentas creadas en los ultimos 30 dias. */
        long registrosUltimos30Dias,

        /** Cuentas que iniciaron sesion en los ultimos 7 dias. La medida real de uso. */
        long activosUltimos7Dias,

        /**
         * Cuentas que nunca iniciaron sesion.
         *
         * Es el numero que mas dice de los ocho. Alguien que se registro y
         * nunca volvio indica que algo se rompio entre el registro y el primer
         * uso, y ese hueco no aparece en ningun otro contador.
         */
        long nuncaIniciaronSesion
) {
}
