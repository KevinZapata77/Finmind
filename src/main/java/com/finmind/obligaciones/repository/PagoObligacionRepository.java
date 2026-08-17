package com.finmind.obligaciones.repository;

import com.finmind.obligaciones.entity.PagoObligacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoObligacionRepository extends JpaRepository<PagoObligacion, Long> {

    List<PagoObligacion> findByObligacionIdOrderByFechaDescIdDesc(Long obligacionId);
}
