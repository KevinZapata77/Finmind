package com.finmind.fijos.controller;

import com.finmind.common.security.UsuarioPrincipal;
import com.finmind.fijos.dto.GastoFijoRequest;
import com.finmind.fijos.dto.GastoFijoResponse;
import com.finmind.fijos.service.ServicioGastosFijos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gastos fijos (RF-046). Base del modulo de alertas. */
@RestController
@RequestMapping("/api/v1/gastos-fijos")
@Tag(name = "Gastos fijos",
        description = "Compromisos que se repiten: arriendo, servicios, suscripciones")
public class GastoFijoController {

    private final ServicioGastosFijos servicio;

    public GastoFijoController(ServicioGastosFijos servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @Operation(summary = "Registrar un gasto fijo", description = "RF-046")
    public ResponseEntity<GastoFijoResponse> crear(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody GastoFijoRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.crear(principal.getId(), peticion));
    }

    @GetMapping
    @Operation(summary = "Listar los gastos fijos propios",
            description = "RF-046. Incluye el equivalente mensual y si ya se cubrio este mes")
    public ResponseEntity<List<GastoFijoResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return ResponseEntity.ok(servicio.listar(principal.getId(), incluirInactivos));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar un gasto fijo", description = "RF-046")
    public ResponseEntity<GastoFijoResponse> actualizar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody GastoFijoRequest peticion) {
        return ResponseEntity.ok(servicio.actualizar(principal.getId(), id, peticion));
    }

    @PatchMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar un gasto fijo",
            description = "RF-046. No se borra: sigue explicando las alertas de meses anteriores")
    public ResponseEntity<GastoFijoResponse> desactivar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(servicio.desactivar(principal.getId(), id));
    }

    @PatchMapping("/{id}/activar")
    @Operation(summary = "Reactivar un gasto fijo", description = "RF-046")
    public ResponseEntity<GastoFijoResponse> activar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(servicio.activar(principal.getId(), id));
    }
}
