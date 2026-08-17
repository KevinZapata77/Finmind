package com.finmind.obligaciones.dto;

import com.finmind.obligaciones.entity.PagoObligacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Un pago ya descompuesto. El aviso aparece cuando el pago no toco el capital. */
public record PagoResponse(
        Long id,
        BigDecimal monto,
        BigDecimal interes,
        BigDecimal abonoCapital,
        BigDecimal saldoResultante,
        LocalDate fecha,
        String descripcion,
        String advertencia
) {
    public static PagoResponse de(PagoObligacion p) {
        String aviso = p.getAbonoCapital().signum() == 0
                ? "Este pago no alcanzo a cubrir el interes del periodo, asi que la deuda no bajo."
                : null;
        return new PagoResponse(p.getId(), p.getMonto(), p.getInteres(), p.getAbonoCapital(),
                p.getSaldoResultante(), p.getFecha(), p.getDescripcion(), aviso);
    }
}
