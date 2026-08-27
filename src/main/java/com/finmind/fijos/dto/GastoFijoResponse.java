package com.finmind.fijos.dto;

import com.finmind.fijos.entity.GastoFijo;

import java.math.BigDecimal;
import java.time.LocalDate;

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
                g.getDiaPago(), g.proximoPagoDesde(hoy), cubierto, g.getActivo());
    }
}
