package com.finmind.reportes.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** RF-022. Composicion del gasto por categoria, ordenada de mayor a menor. */
public record ComposicionResponse(
        BigDecimal total,
        List<Porcion> porciones
) {
    /** Una categoria con su monto y su peso sobre el total. */
    public record Porcion(Long categoriaId, String nombre, String color,
                          BigDecimal monto, BigDecimal porcentaje) {
    }

    public static ComposicionResponse de(List<Object[]> filas) {
        BigDecimal total = filas.stream()
                .map(f -> (BigDecimal) f[3])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Porcion> porciones = filas.stream().map(f -> {
            BigDecimal monto = (BigDecimal) f[3];
            // Con total cero no hay division posible; tampoco hay nada que mostrar.
            BigDecimal pct = total.signum() == 0
                    ? BigDecimal.ZERO.setScale(1)
                    : monto.multiply(new BigDecimal("100")).divide(total, 1, RoundingMode.HALF_UP);
            return new Porcion((Long) f[0], (String) f[1], (String) f[2], monto, pct);
        }).toList();

        return new ComposicionResponse(total, porciones);
    }
}
