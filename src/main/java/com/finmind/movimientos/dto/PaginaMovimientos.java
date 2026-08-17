package com.finmind.movimientos.dto;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pagina de movimientos con los totales del filtro aplicado.
 *
 * Los totales corresponden a TODO lo filtrado, no solo a la pagina visible:
 * un total que cambiara al pasar de pagina no significaria nada.
 */
public record PaginaMovimientos(
        List<MovimientoResponse> contenido,
        int pagina, int tamano, long totalElementos, int totalPaginas,
        BigDecimal totalIngresos, BigDecimal totalGastos, BigDecimal diferencia
) {
    public static PaginaMovimientos de(Page<?> p, List<MovimientoResponse> filas,
                                       BigDecimal ingresos, BigDecimal gastos) {
        return new PaginaMovimientos(filas, p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages(),
                ingresos, gastos, ingresos.subtract(gastos));
    }
}
