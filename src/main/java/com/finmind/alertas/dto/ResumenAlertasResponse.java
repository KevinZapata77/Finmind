package com.finmind.alertas.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * RF-047. Las alertas del mes con los numeros que las sustentan.
 *
 * Las cifras viajan aparte de los mensajes a proposito: el cliente puede
 * pintarlas en un grafico, y el usuario que desconfia de un aviso puede
 * comprobar de donde sale.
 */
public record ResumenAlertasResponse(
        List<AlertaResponse> alertas,
        /** Dias que ya pasaron del mes y dias que tiene el mes. */
        int diasTranscurridos,
        int diasDelMes,
        BigDecimal ingresosDelMes,
        BigDecimal gastadoHastaHoy,
        /** Gasto medio por dia en lo que va del mes. */
        BigDecimal ritmoDiario,
        /** A este ritmo, cuanto se habra gastado al cerrar el mes. */
        BigDecimal proyeccionFinDeMes,
        /** Compromisos fijos del mes que todavia no se han cubierto. */
        BigDecimal compromisosPendientes,
        /** Ingresos menos lo gastado: lo que queda hoy. */
        BigDecimal disponible,
        /** Disponible menos compromisos pendientes. Negativo significa problema. */
        BigDecimal holgura,
        /** Frase que resume el mes, para no depender del color. */
        String lectura
) {
}
