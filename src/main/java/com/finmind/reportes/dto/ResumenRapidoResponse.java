package com.finmind.reportes.dto;

import java.math.BigDecimal;

/**
 * Lo que entro y salio hoy, esta semana y este mes.
 *
 * Existe para que la pantalla principal pueda responder de un vistazo la
 * pregunta que la gente se hace todos los dias -- "cuanto llevo hoy" -- sin
 * tener que abrir el listado de movimientos ni elegir un periodo.
 *
 * Va en una sola llamada y no en tres: en la pantalla de inicio, tres viajes al
 * servidor se notan y podrian llegar desincronizados entre si.
 */
public record ResumenRapidoResponse(
        Periodo hoy,
        Periodo semana,
        Periodo mes
) {
    public record Periodo(BigDecimal ingresos, BigDecimal gastos, BigDecimal neto) {
        public static Periodo de(BigDecimal ingresos, BigDecimal gastos) {
            return new Periodo(ingresos, gastos, ingresos.subtract(gastos));
        }
    }
}
