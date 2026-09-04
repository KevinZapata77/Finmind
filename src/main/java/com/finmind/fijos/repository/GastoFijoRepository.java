package com.finmind.fijos.repository;

import com.finmind.fijos.entity.GastoFijo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GastoFijoRepository extends JpaRepository<GastoFijo, Long> {

    /*
     * PERF-01. La categoria viaja en la misma consulta.
     *
     * GastoFijoResponse lee categoria.nombre y categoria.colorHex, y la
     * relacion es LAZY: cada categoria distinta de la lista costaba una
     * consulta suelta. Contra Neon en Ohio cada una son 70-100 ms.
     *
     * Se usa @EntityGraph y no un JOIN FETCH escrito a mano porque estas
     * consultas son derivadas del nombre del metodo. Reescribirlas como @Query
     * obligaria a repetir el filtro y el orden en texto, y a mantener los dos
     * sincronizados.
     */
    @EntityGraph(attributePaths = "categoria")
    List<GastoFijo> findByUsuarioIdOrderByDiaPagoAsc(Long usuarioId);

    @EntityGraph(attributePaths = "categoria")
    List<GastoFijo> findByUsuarioIdAndActivoTrueOrderByDiaPagoAsc(Long usuarioId);

    Optional<GastoFijo> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndNombreIgnoreCase(Long usuarioId, String nombre);

    boolean existsByUsuarioIdAndNombreIgnoreCaseAndIdNot(Long usuarioId, String nombre, Long id);
}
