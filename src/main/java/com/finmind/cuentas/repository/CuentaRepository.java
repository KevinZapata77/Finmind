package com.finmind.cuentas.repository;

import com.finmind.cuentas.entity.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    /**
     * RN-005. Se busca por id Y por dueño en la misma consulta.
     * Traer la cuenta y despues comprobar el dueño en Java funciona igual,
     * pero deja la puerta abierta a que alguien olvide la comprobacion.
     * Aqui es imposible obtener una cuenta ajena.
     */
    Optional<Cuenta> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Cuenta> findByUsuarioIdOrderByNombreAsc(Long usuarioId);

    List<Cuenta> findByUsuarioIdAndActivaTrueOrderByNombreAsc(Long usuarioId);

    boolean existsByUsuarioIdAndNombreIgnoreCase(Long usuarioId, String nombre);

    /** Para editar: el nombre puede repetirse consigo mismo, no con otras. */
    boolean existsByUsuarioIdAndNombreIgnoreCaseAndIdNot(Long usuarioId, String nombre, Long id);

}
