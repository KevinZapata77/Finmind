package com.finmind.reportes.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * RF-048. Como se acumulo el dinero dia por dia dentro del mes.
 *
 * POR QUE HACE FALTA UN ENDPOINT PARA ESTO
 * El panel ya sabia el total del mes, y con un total se puede dibujar una barra
 * pero no una linea. Para trazar la curva del gasto hay que saber cuanto se
 * llevaba cada dia, y eso no se puede deducir del total: dibujar una recta
 * desde cero hasta el total seria inventar un dato — diria que el usuario gasto
 * lo mismo todos los dias, que es justo lo que el grafico deberia revelar que
 * no pasa.
 *
 * QUE SE DEVUELVE
 * Un punto por cada dia transcurrido, con lo acumulado hasta ese dia. Los dias
 * sin movimientos tambien vienen, repitiendo el acumulado anterior: si se
 * omitieran, la linea uniria el dia 3 con el dia 9 y el tramo plano — seis dias
 * sin gastar, que es informacion — desapareceria.
 */
public record RitmoResponse(
        /** Un punto por dia, del 1 hasta hoy (o hasta fin de mes si ya paso). */
        List<Dia> dias,
        /** Dias que tiene el mes. Fija el eje horizontal del grafico. */
        int diasDelMes,
        /** Ultimo dia con datos. Igual a diasDelMes en los meses cerrados. */
        int diasTranscurridos,
        /**
         * Donde terminaria el gasto si el ritmo se mantuviera. Permite dibujar
         * la linea punteada hacia el futuro. Nula en los meses ya cerrados,
         * donde proyectar no significa nada.
         */
        BigDecimal proyeccionGasto,
        BigDecimal totalIngresos,
        BigDecimal totalGastos
) {
    /** Acumulado al cierre de ese dia, no el movimiento de ese dia. */
    public record Dia(int dia, BigDecimal ingresoAcumulado, BigDecimal gastoAcumulado) {
    }
}
