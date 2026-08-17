package com.finmind.presupuestos.controller;

import com.finmind.common.security.UsuarioPrincipal;
import com.finmind.presupuestos.dto.*;
import com.finmind.presupuestos.service.ServicioPresupuestos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Presupuestos (RF-017 a RF-020). */
@RestController
@RequestMapping("/api/v1/presupuestos")
@Tag(name = "Presupuestos", description = "Limites de gasto por categoria y mes")
public class PresupuestoController {

    private final ServicioPresupuestos servicio;

    public PresupuestoController(ServicioPresupuestos servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @Operation(summary = "Definir un presupuesto", description = "RF-017")
    public ResponseEntity<PresupuestoResponse> crear(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody PresupuestoRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(principal.getId(), peticion));
    }

    @GetMapping
    @Operation(summary = "Consultar presupuestos y su consumo", description = "RF-018, RF-019")
    public ResponseEntity<List<PresupuestoResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) Short anio,
            @RequestParam(required = false) Short mes) {
        return ResponseEntity.ok(servicio.listar(principal.getId(), anio, mes));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un presupuesto", description = "RF-018")
    public ResponseEntity<PresupuestoResponse> consultar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(servicio.consultar(principal.getId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cambiar el limite", description = "RF-020. La categoria y el periodo no se editan")
    public ResponseEntity<PresupuestoResponse> editar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody EditarPresupuestoRequest peticion) {
        return ResponseEntity.ok(servicio.editar(principal.getId(), id, peticion));
    }

    @PatchMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar un presupuesto", description = "RF-020")
    public ResponseEntity<PresupuestoResponse> desactivar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(servicio.desactivar(principal.getId(), id));
    }

    @PatchMapping("/{id}/activar")
    @Operation(summary = "Reactivar un presupuesto", description = "RF-020")
    public ResponseEntity<PresupuestoResponse> activar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(servicio.activar(principal.getId(), id));
    }
}
