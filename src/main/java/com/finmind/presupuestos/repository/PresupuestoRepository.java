package com.finmind.presupuestos.repository;

import com.finmind.presupuestos.entity.Presupuesto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {

    /** RN-005. */
    Optional<Presupuesto> findByIdAndUsuarioId(Long id, Long usuarioId);

    /*
     * PERF-01. La categoria viaja en la misma consulta.
     *
     * PresupuestoResponse lee categoria.nombre y categoria.colorHex, y la
     * relacion es LAZY: una consulta suelta por cada categoria de la lista.
     *
     * Aqui el desperdicio era doble: el orden es OrderByCategoriaNombreAsc, o
     * sea que la base YA estaba uniendo la tabla de categorias para poder
     * ordenar, y despues Hibernate volvia a pedir cada fila una por una. El
     * @EntityGraph solo le pide que se traiga lo que ya estaba mirando.
     */
    @EntityGraph(attributePaths = "categoria")
    List<Presupuesto> findByUsuarioIdAndAnioAndMesOrderByCategoriaNombreAsc(
            Long usuarioId, Short anio, Short mes);

    /** RN-006: a lo sumo un presupuesto por usuario, categoria, anio y mes. */
    boolean existsByUsuarioIdAndCategoriaIdAndAnioAndMes(
            Long usuarioId, Long categoriaId, Short anio, Short mes);
}
