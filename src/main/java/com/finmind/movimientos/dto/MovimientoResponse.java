package com.finmind.movimientos.dto;

import com.finmind.movimientos.entity.Transaccion;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Trae los nombres resueltos para que la pantalla no tenga que pedirlos aparte. */
public record MovimientoResponse(
        Long id, String tipo, BigDecimal monto, LocalDate fecha, String descripcion,
        Long cuentaId, String cuentaNombre,
        Long categoriaId, String categoriaNombre, String categoriaColor
) {
    public static MovimientoResponse de(Transaccion t) {
        return new MovimientoResponse(
                t.getId(), t.getTipo(), t.getMonto(), t.getFecha(), t.getDescripcion(),
                t.getCuenta().getId(), t.getCuenta().getNombre(),
                t.getCategoria().getId(), t.getCategoria().getNombre(), t.getCategoria().getColorHex());
    }
}
