package com.finmind.identidad.repository;

import com.finmind.identidad.entity.CodigoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

    /** El unico codigo sin usar de ese usuario para ese flujo. */
    Optional<CodigoVerificacion> findByUsuarioIdAndTipoAndUsadoEnIsNull(Long usuarioId, String tipo);

    /**
     * Invalida el codigo anterior antes de emitir uno nuevo.
     * Sostiene el indice unico uk_codigo_vigente y corta el abuso del reenvio (AME-07).
     */
    @Modifying
    @Query("update CodigoVerificacion c set c.usadoEn = :ahora "
         + "where c.usuario.id = :usuarioId and c.tipo = :tipo and c.usadoEn is null")
    int invalidarAnteriores(@Param("usuarioId") Long usuarioId,
                            @Param("tipo") String tipo,
                            @Param("ahora") LocalDateTime ahora);
}
