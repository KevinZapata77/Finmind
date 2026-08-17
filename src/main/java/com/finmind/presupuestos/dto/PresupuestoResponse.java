package com.finmind.presupuestos.dto;

import com.finmind.presupuestos.entity.Presupuesto;

import java.math.BigDecimal;

/**
 * RF-018 y RF-019.
 *
 * `estado` y `aviso` van en texto ademas del numero: un semaforo que solo se
 * distinga por color deja fuera a quien no distingue rojo de verde.
 */
public record PresupuestoResponse(
        Long id, Long categoriaId, String categoriaNombre, String categoriaColor,
        BigDecimal montoLimite, BigDecimal consumo, BigDecimal disponible,
        BigDecimal porcentajeConsumido, Short anio, Short mes, String periodo,
        Boolean activo, String estado, String aviso
) {
    public static PresupuestoResponse de(Presupuesto p, BigDecimal consumo) {
        BigDecimal pct = p.porcentajeConsumido(consumo);
        BigDecimal disponible = p.disponible(consumo);

        String estado;
        String aviso;
        if (pct.compareTo(new BigDecimal("100")) > 0) {
            estado = "EXCEDIDO";
            aviso = "Te pasaste del limite en " + disponible.abs() + ".";
        } else if (pct.compareTo(Presupuesto.UMBRAL_ALERTA) >= 0) {
            estado = "EN_ALERTA";
            aviso = "Ya usaste el " + pct + "% de este presupuesto. Te quedan " + disponible + ".";
        } else {
            estado = "EN_CURSO";
            aviso = null;
        }

        return new PresupuestoResponse(p.getId(), p.getCategoria().getId(),
                p.getCategoria().getNombre(), p.getCategoria().getColorHex(),
                p.getMontoLimite(), consumo, disponible, pct,
                p.getAnio(), p.getMes(), p.getPeriodo(), p.getActivo(), estado, aviso);
    }
}
