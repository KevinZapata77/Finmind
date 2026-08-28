package com.finmind.fijos.dto;

import com.finmind.fijos.entity.GastoFijo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Vista de un compromiso recurrente.
 *
 * Trae el equivalente mensual ya calculado porque es el numero con el que el
 * usuario compara: un gasto semanal de 50.000 pesa 217.250 al mes, y esa
 * conversion no deberia tener que hacerla el.
 */
public record GastoFijoResponse(
        Long id,
        String nombre,
        Long categoriaId,
        String categoriaNombre,
        String categoriaColor,
        BigDecimal monto,
        String periodicidad,
        /** Lo que este compromiso representa al mes (RN-025). */
        BigDecimal montoMensual,
        Short diaPago,
        LocalDate proximoPago,
        /**
         * DEF-21. Todas las fechas de pago del mes en curso.
         *
         * En un quincenal son dos, y mostrar solo la proxima lo hacia ver como
         * mensual: el monto mensual contaba dos pagos pero el calendario
         * enseñaba uno. Vacia en los semanales, donde enumerar cuatro o cinco
         * fechas no aporta nada sobre "cada viernes".
         */
        List<LocalDate> pagosDelMes,
        /** True si el compromiso ya fue cubierto por el gasto real de su categoria. */
        Boolean cubiertoEsteMes,
        Boolean activo
) {
    public static GastoFijoResponse de(GastoFijo g, LocalDate hoy, boolean cubierto) {
        return new GastoFijoResponse(
                g.getId(), g.getNombre(),
                g.getCategoria().getId(), g.getCategoria().getNombre(),
                g.getCategoria().getColorHex(),
                g.getMonto(), g.getPeriodicidad(), g.montoMensualEquivalente(),
                g.getDiaPago(), g.proximoPagoDesde(hoy),
                g.fechasDelMes(YearMonth.from(hoy)), cubierto, g.getActivo());
    }
}
