package com.finmind.presupuestos.repository;

import com.finmind.presupuestos.entity.Presupuesto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {

    /** RN-005. */
    Optional<Presupuesto> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Presupuesto> findByUsuarioIdAndAnioAndMesOrderByCategoriaNombreAsc(
            Long usuarioId, Short anio, Short mes);

    /** RN-006: a lo sumo un presupuesto por usuario, categoria, anio y mes. */
    boolean existsByUsuarioIdAndCategoriaIdAndAnioAndMes(
            Long usuarioId, Long categoriaId, Short anio, Short mes);
}
