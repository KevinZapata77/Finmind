package com.finmind.movimientos.dto;

import com.finmind.cuentas.entity.Cuenta;
import com.finmind.movimientos.entity.Transaccion;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Trae los nombres resueltos para que la pantalla no tenga que pedirlos aparte.
 *
 * En una TRANSFERENCIA la categoria es nula y en cambio hay cuenta de destino.
 * Los tres campos de categoria viajan nulos y el cliente los omite: una
 * transferencia no pertenece a ninguna categoria de gasto porque el dinero no
 * se consumio, cambio de sitio.
 */
public record MovimientoResponse(
        Long id, String tipo, BigDecimal monto, LocalDate fecha, String descripcion,
        Long cuentaId, String cuentaNombre,
        Long categoriaId, String categoriaNombre, String categoriaColor,
        /** Solo en las transferencias: a donde fue el dinero. */
        Long cuentaDestinoId, String cuentaDestinoNombre
) {
    public static MovimientoResponse de(Transaccion t) {
        // Sin estos nulos, listar una transferencia reventaba con
        // NullPointerException al pedirle el identificador a una categoria que
        // no existe.
        boolean conCategoria = t.getCategoria() != null;
        Cuenta destino = t.getCuentaDestino();

        return new MovimientoResponse(
                t.getId(), t.getTipo(), t.getMonto(), t.getFecha(), t.getDescripcion(),
                t.getCuenta().getId(), t.getCuenta().getNombre(),
                conCategoria ? t.getCategoria().getId() : null,
                conCategoria ? t.getCategoria().getNombre() : null,
                conCategoria ? t.getCategoria().getColorHex() : null,
                destino == null ? null : destino.getId(),
                destino == null ? null : destino.getNombre());
    }
}
