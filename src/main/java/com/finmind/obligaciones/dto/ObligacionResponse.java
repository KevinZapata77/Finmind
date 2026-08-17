package com.finmind.obligaciones.dto;

import com.finmind.obligaciones.entity.Obligacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Vista publica de una obligacion. Nunca expone al usuario dueno. */
public record ObligacionResponse(
        Long id,
        String nombre,
        String acreedor,
        String tipo,
        BigDecimal montoOriginal,
        BigDecimal saldoPendiente,
        BigDecimal tasaAnual,
        BigDecimal cuotaMensual,
        BigDecimal interesDelPeriodo,
        BigDecimal porcentajePagado,
        Short diaPago,
        LocalDate proximoPago,
        boolean venceEnSieteDias,
        LocalDate fechaInicio,
        String estado
) {
    public static ObligacionResponse de(Obligacion o, LocalDate proximoPago, boolean proxima) {
        return new ObligacionResponse(
                o.getId(), o.getNombre(), o.getAcreedor(), o.getTipo(),
                o.getMontoOriginal(), o.getSaldoPendiente(), o.getTasaAnual(),
                o.getCuotaMensual(), o.interesDelPeriodo(), o.porcentajePagado(),
                o.getDiaPago(), proximoPago, proxima, o.getFechaInicio(), o.getEstado());
    }
}
