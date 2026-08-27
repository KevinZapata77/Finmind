package com.finmind.reportes.controller;

import com.finmind.common.security.UsuarioPrincipal;
import com.finmind.reportes.dto.*;
import com.finmind.reportes.service.ServicioReportes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Balance, composicion del gasto y panel (RF-021, RF-022). */
@RestController
@RequestMapping("/api/v1/reportes")
@Tag(name = "Reportes", description = "Balance del periodo y composicion del gasto")
public class ReporteController {

    private final ServicioReportes servicio;

    public ReporteController(ServicioReportes servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/balance")
    @Operation(summary = "Ingresos, gastos y diferencia del periodo", description = "RF-021")
    public ResponseEntity<BalanceResponse> balance(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) Short anio,
            @RequestParam(required = false) Short mes) {
        return ResponseEntity.ok(servicio.balance(principal.getId(), anio, mes));
    }

    @GetMapping("/gasto-por-categoria")
    @Operation(summary = "Composicion del gasto del periodo", description = "RF-022")
    public ResponseEntity<ComposicionResponse> composicion(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) Short anio,
            @RequestParam(required = false) Short mes) {
        return ResponseEntity.ok(servicio.gastoPorCategoria(principal.getId(), anio, mes));
    }

    @GetMapping("/resumen-rapido")
    @Operation(summary = "Lo de hoy, la semana y el mes", description = "RF-040. Alimenta el registro rapido")
    public ResponseEntity<ResumenRapidoResponse> resumenRapido(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(servicio.resumenRapido(principal.getId()));
    }

    @GetMapping("/panel")
    @Operation(summary = "Todo el panel en una sola llamada", description = "RF-021, RF-022, RF-038")
    public ResponseEntity<PanelResponse> panel(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) Short anio,
            @RequestParam(required = false) Short mes) {
        return ResponseEntity.ok(servicio.panel(principal.getId(), anio, mes));
    }

    @GetMapping("/ritmo")
    @Operation(summary = "Acumulado dia por dia del mes",
            description = "RF-048. Un punto por cada dia transcurrido, con el ingreso y el "
                    + "gasto acumulados hasta ese dia. Es lo que permite trazar la curva del "
                    + "mes: con solo el total no se puede dibujar una linea sin inventarla")
    public ResponseEntity<RitmoResponse> ritmo(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) Short anio,
            @RequestParam(required = false) Short mes) {
        return ResponseEntity.ok(servicio.ritmo(principal.getId(), anio, mes));
    }
}
