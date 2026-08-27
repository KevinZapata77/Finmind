package com.finmind.alertas.controller;

import com.finmind.alertas.dto.ResumenAlertasResponse;
import com.finmind.alertas.service.ServicioAlertas;
import com.finmind.common.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Alertas del mes (RF-047). */
@RestController
@RequestMapping("/api/v1/alertas")
@Tag(name = "Alertas",
        description = "Avisos sobre como va el mes, calculados al vuelo desde los movimientos, "
                + "los presupuestos y los compromisos fijos")
public class AlertaController {

    private final ServicioAlertas servicio;

    public AlertaController(ServicioAlertas servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Alertas y proyeccion del mes en curso",
            description = "RF-047, RN-027 a RN-031. Devuelve los avisos y las cifras que los "
                    + "sustentan: ritmo de gasto, proyeccion de cierre y compromisos pendientes")
    public ResponseEntity<ResumenAlertasResponse> delMes(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(servicio.delMes(principal.getId()));
    }
}
