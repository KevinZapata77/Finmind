package com.finmind.metas.repository;

import com.finmind.metas.entity.MetaAhorro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetaAhorroRepository extends JpaRepository<MetaAhorro, Long> {

    /** RN-005. */
    Optional<MetaAhorro> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<MetaAhorro> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    List<MetaAhorro> findByUsuarioIdAndEstadoOrderByFechaCreacionDesc(Long usuarioId, String estado);

    boolean existsByUsuarioIdAndNombreIgnoreCase(Long usuarioId, String nombre);

    boolean existsByUsuarioIdAndNombreIgnoreCaseAndIdNot(Long usuarioId, String nombre, Long id);
}
