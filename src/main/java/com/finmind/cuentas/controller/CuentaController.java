package com.finmind.cuentas.controller;

import com.finmind.common.security.UsuarioPrincipal;
import com.finmind.cuentas.dto.ActualizarCuentaRequest;
import com.finmind.cuentas.dto.CrearCuentaRequest;
import com.finmind.cuentas.dto.CuentaResponse;
import com.finmind.cuentas.service.ServicioCuentas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Cuentas financieras del usuario (API-04, API-15 a API-19).
 *
 * El usuario sale SIEMPRE de @AuthenticationPrincipal, o sea del token firmado.
 * Ninguna ruta recibe el usuario por parametro: si lo hiciera, bastaria cambiar
 * un numero en la URL para ver las cuentas de otro (RN-005).
 */
@RestController
@RequestMapping("/api/v1/cuentas")
@Tag(name = "Cuentas", description = "Cuentas financieras del usuario autenticado")
public class CuentaController {

    private final ServicioCuentas servicio;

    public CuentaController(ServicioCuentas servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @Operation(summary = "Crear una cuenta", description = "RF-006")
    public ResponseEntity<CuentaResponse> crear(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CrearCuentaRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.crear(principal.getId(), peticion));
    }

    @GetMapping
    @Operation(summary = "Listar las cuentas propias con su saldo", description = "RF-007")
    public ResponseEntity<List<CuentaResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(defaultValue = "false") boolean incluirInactivas) {
        return ResponseEntity.ok(servicio.listar(principal.getId(), incluirInactivas));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una cuenta propia", description = "RF-007")
    public ResponseEntity<CuentaResponse> consultar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(servicio.consultar(principal.getId(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar el nombre y el tipo de una cuenta", description = "RF-008")
    public ResponseEntity<CuentaResponse> actualizar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ActualizarCuentaRequest peticion) {
        return ResponseEntity.ok(servicio.actualizar(principal.getId(), id, peticion));
    }

    @PatchMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar una cuenta", description = "RF-008. No se borra: el historial la sigue referenciando")
    public ResponseEntity<CuentaResponse> desactivar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(servicio.desactivar(principal.getId(), id));
    }

    @PatchMapping("/{id}/activar")
    @Operation(summary = "Reactivar una cuenta", description = "RF-008")
    public ResponseEntity<CuentaResponse> activar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(servicio.activar(principal.getId(), id));
    }
}
