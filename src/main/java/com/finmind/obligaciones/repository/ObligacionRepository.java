package com.finmind.obligaciones.repository;

import com.finmind.obligaciones.entity.Obligacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ObligacionRepository extends JpaRepository<Obligacion, Long> {

    /** RN-005: id y dueno en la misma consulta, para que no exista la via de olvidarlo. */
    Optional<Obligacion> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Obligacion> findByUsuarioIdOrderByNombreAsc(Long usuarioId);

    List<Obligacion> findByUsuarioIdAndEstadoOrderByNombreAsc(Long usuarioId, String estado);

    boolean existsByUsuarioIdAndNombreIgnoreCase(Long usuarioId, String nombre);

    boolean existsByUsuarioIdAndNombreIgnoreCaseAndIdNot(Long usuarioId, String nombre, Long id);

    /** RF-038: total adeudado, para el patrimonio neto. Se suma en la base, no en memoria. */
    @Query("""
           SELECT COALESCE(SUM(o.saldoPendiente), 0) FROM Obligacion o
           WHERE o.usuario.id = :usuarioId AND o.estado = 'ACTIVA'
           """)
    BigDecimal totalAdeudado(@Param("usuarioId") Long usuarioId);
}
