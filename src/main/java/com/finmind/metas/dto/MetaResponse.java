package com.finmind.metas.dto;

import com.finmind.metas.entity.MetaAhorro;

import java.math.BigDecimal;
import java.time.LocalDate;

/** El mensaje va en texto para no depender solo de la barra de progreso. */
public record MetaResponse(
        Long id, String nombre, BigDecimal montoObjetivo, BigDecimal montoActual,
        BigDecimal loQueFalta, BigDecimal porcentajeAvance,
        LocalDate fechaLimite, String estado, String lectura
) {
    public static MetaResponse de(MetaAhorro m) {
        String lectura = switch (m.getEstado()) {
            case MetaAhorro.COMPLETADA -> "Meta cumplida. Ahorraste " + m.getMontoActual() + ".";
            case MetaAhorro.CANCELADA -> "Meta cancelada. Lo ahorrado sigue registrado.";
            default -> "Te faltan " + m.loQueFalta() + " para lograrla.";
        };
        return new MetaResponse(m.getId(), m.getNombre(), m.getMontoObjetivo(), m.getMontoActual(),
                m.loQueFalta(), m.porcentajeAvance(), m.getFechaLimite(), m.getEstado(), lectura);
    }
}
