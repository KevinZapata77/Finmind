package com.finmind.reportes.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** RF-022. Composicion del gasto por categoria, ordenada de mayor a menor. */
public record ComposicionResponse(
        BigDecimal total,
        List<Porcion> porciones
) {
    /** Una categoria con su monto y su peso sobre el total. */
    public record Porcion(Long categoriaId, String nombre, String color,
                          BigDecimal monto, BigDecimal porcentaje,

                          /**
                           * RF-050. Lo gastado en esta misma categoria el mes
                           * anterior, y la diferencia.
                           *
                           * Ambos son null cuando no hay periodo previo con el
                           * cual comparar (RN-032). Null no es cero: cero dice
                           * "el mes pasado no gastaste nada aqui", y eso
                           * convertiria cualquier gasto nuevo en un aumento del
                           * infinito por ciento.
                           */
                          BigDecimal montoMesAnterior,
                          BigDecimal variacion) {
    }

    /**
     * @param filasMesAnterior las mismas filas del mes previo, o null si no se
     *                         quiere comparacion. Se recibe ya consultado y no
     *                         se consulta aqui: un DTO que va a la base deja de
     *                         ser un DTO.
     */
    public static ComposicionResponse de(List<Object[]> filas, List<Object[]> filasMesAnterior) {
        BigDecimal total = filas.stream()
                .map(f -> (BigDecimal) f[3])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // categoriaId -> gasto del mes anterior. Se arma una sola vez en vez de
        // recorrer la lista anterior por cada porcion.
        Map<Long, BigDecimal> anterior = filasMesAnterior == null ? null
                : filasMesAnterior.stream().collect(Collectors.toMap(
                        f -> (Long) f[0], f -> (BigDecimal) f[3], BigDecimal::add));

        List<Porcion> porciones = filas.stream().map(f -> {
            BigDecimal monto = (BigDecimal) f[3];
            // Con total cero no hay division posible; tampoco hay nada que mostrar.
            BigDecimal pct = total.signum() == 0
                    ? BigDecimal.ZERO.setScale(1)
                    : monto.multiply(new BigDecimal("100")).divide(total, 1, RoundingMode.HALF_UP);

            Long categoriaId = (Long) f[0];
            // Si se pidio comparacion y la categoria no aparecia el mes pasado,
            // el gasto anterior fue cero de verdad: la categoria existia y no se
            // uso. Eso si es comparable.
            BigDecimal antes = anterior == null ? null
                    : anterior.getOrDefault(categoriaId, BigDecimal.ZERO);
            BigDecimal variacion = antes == null ? null : monto.subtract(antes);

            return new Porcion(categoriaId, (String) f[1], (String) f[2], monto, pct,
                    antes, variacion);
        }).toList();

        return new ComposicionResponse(total, porciones);
    }
}
