package com.finmind.movimientos.controller;

import com.finmind.common.security.UsuarioPrincipal;
import com.finmind.movimientos.dto.*;
import com.finmind.movimientos.service.ServicioMovimientos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Movimientos: ingresos y gastos (RF-012 a RF-016). */
@RestController
@RequestMapping("/api/v1/transacciones")
@Tag(name = "Movimientos", description = "Ingresos y gastos del usuario")
public class MovimientoController {

    private final ServicioMovimientos servicio;

    public MovimientoController(ServicioMovimientos servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @Operation(summary = "Registrar un ingreso o un gasto", description = "RF-012, RF-013")
    public ResponseEntity<MovimientoResponse> registrar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody MovimientoRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.registrar(principal.getId(), peticion));
    }

    @GetMapping
    @Operation(summary = "Consultar movimientos con filtros", description = "RF-014")
    public ResponseEntity<PaginaMovimientos> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long cuentaId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(servicio.listar(principal.getId(), desde, hasta,
                cuentaId, categoriaId, tipo, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un movimiento", description = "RF-014")
    public ResponseEntity<MovimientoResponse> consultar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(servicio.consultar(principal.getId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar un movimiento", description = "RF-015")
    public ResponseEntity<MovimientoResponse> actualizar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody MovimientoRequest peticion) {
        return ResponseEntity.ok(servicio.actualizar(principal.getId(), id, peticion));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un movimiento", description = "RF-016")
    public ResponseEntity<Void> eliminar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        servicio.eliminar(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
