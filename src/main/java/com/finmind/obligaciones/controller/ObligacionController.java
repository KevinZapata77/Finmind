package com.finmind.obligaciones.controller;

import com.finmind.common.security.UsuarioPrincipal;
import com.finmind.cuentas.service.ServicioCuentas;
import com.finmind.obligaciones.dto.*;
import com.finmind.obligaciones.service.ServicioObligaciones;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Obligaciones financieras del usuario autenticado (API-20 a API-26). */
@RestController
@RequestMapping("/api/v1/obligaciones")
@Tag(name = "Obligaciones", description = "Deudas del usuario, sus pagos y el patrimonio neto")
public class ObligacionController {

    private final ServicioObligaciones servicio;
    private final ServicioCuentas cuentas;

    public ObligacionController(ServicioObligaciones servicio, ServicioCuentas cuentas) {
        this.servicio = servicio;
        this.cuentas = cuentas;
    }

    @PostMapping
    @Operation(summary = "Registrar una obligacion", description = "RF-035")
    public ResponseEntity<ObligacionResponse> crear(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CrearObligacionRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.crear(principal.getId(), peticion));
    }

    @GetMapping
    @Operation(summary = "Listar las obligaciones propias", description = "RF-037, RF-039")
    public ResponseEntity<List<ObligacionResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(defaultValue = "true") boolean soloActivas) {
        return ResponseEntity.ok(servicio.listar(principal.getId(), soloActivas));
    }

    /**
     * RF-038. Va antes de /{id} a proposito: si estuviera despues, Spring
     * intentaria interpretar "patrimonio" como un identificador.
     */
    @GetMapping("/patrimonio")
    @Operation(summary = "Activos, obligaciones y patrimonio neto", description = "RF-038, RN-020")
    public ResponseEntity<PatrimonioResponse> patrimonio(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Long id = principal.getId();
        return ResponseEntity.ok(servicio.patrimonio(
                id, cuentas.totalActivos(id), cuentas.totalDeudaEnTarjetas(id)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una obligacion", description = "RF-037")
    public ResponseEntity<ObligacionResponse> consultar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(servicio.consultar(principal.getId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar nombre, acreedor, cuota y dia de pago", description = "RF-035")
    public ResponseEntity<ObligacionResponse> actualizar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody ActualizarObligacionRequest peticion) {
        return ResponseEntity.ok(servicio.actualizar(principal.getId(), id, peticion));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar una obligacion", description = "RF-037. No se borra: conserva su historial")
    public ResponseEntity<ObligacionResponse> cancelar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(servicio.cancelar(principal.getId(), id));
    }

    @PostMapping("/{id}/pagos")
    @Operation(summary = "Registrar un pago", description = "RF-036. El desglose lo calcula el servidor (RN-018)")
    public ResponseEntity<PagoResponse> pagar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody RegistrarPagoRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.registrarPago(principal.getId(), id, peticion));
    }

    @GetMapping("/{id}/pagos")
    @Operation(summary = "Historial de pagos de una obligacion", description = "RF-036")
    public ResponseEntity<List<PagoResponse>> historial(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(servicio.historial(principal.getId(), id));
    }
}
