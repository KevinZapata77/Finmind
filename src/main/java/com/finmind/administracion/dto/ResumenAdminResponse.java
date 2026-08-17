package com.finmind.administracion.dto;

/** Conteos de la plataforma. Numeros agregados, nunca datos de una persona. */
public record ResumenAdminResponse(
        long total, long activos, long sinVerificar, long desactivados
) {
}
