package com.finmind.fijos.repository;

import com.finmind.fijos.entity.GastoFijo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GastoFijoRepository extends JpaRepository<GastoFijo, Long> {

    List<GastoFijo> findByUsuarioIdOrderByDiaPagoAsc(Long usuarioId);

    List<GastoFijo> findByUsuarioIdAndActivoTrueOrderByDiaPagoAsc(Long usuarioId);

    Optional<GastoFijo> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndNombreIgnoreCase(Long usuarioId, String nombre);

    boolean existsByUsuarioIdAndNombreIgnoreCaseAndIdNot(Long usuarioId, String nombre, Long id);
}
