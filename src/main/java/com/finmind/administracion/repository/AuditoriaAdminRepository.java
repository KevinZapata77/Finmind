package com.finmind.administracion.repository;

import com.finmind.administracion.entity.AuditoriaAdmin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Solo lectura y alta: la auditoria no se edita ni se borra. */
public interface AuditoriaAdminRepository extends JpaRepository<AuditoriaAdmin, Long> {

    Page<AuditoriaAdmin> findAllByOrderByFechaDescIdDesc(Pageable pagina);
}
