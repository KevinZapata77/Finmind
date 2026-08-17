package com.finmind.administracion.dto;

import com.finmind.administracion.entity.AuditoriaAdmin;

import java.time.LocalDateTime;

/** Una linea del registro de auditoria (RF-024). */
public record AuditoriaResponse(
        Long id, String adminCorreo, String accion, String entidad,
        Long entidadId, String detalle, LocalDateTime fecha
) {
    public static AuditoriaResponse de(AuditoriaAdmin a) {
        return new AuditoriaResponse(a.getId(), a.getAdmin().getCorreo(), a.getAccion(),
                a.getEntidad(), a.getEntidadId(), a.getDetalle(), a.getFecha());
    }
}
