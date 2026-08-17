package com.finmind.administracion.controller;

import com.finmind.administracion.dto.*;
import com.finmind.administracion.service.ServicioAdministracion;
import com.finmind.common.security.UsuarioPrincipal;
import com.finmind.usuarios.entity.Rol;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Administracion de usuarios (RF-023, RF-024).
 *
 * @PreAuthorize a nivel de clase: TODO lo de aqui exige rol de administrador.
 * Ponerlo por metodo dejaria la puerta abierta a olvidarlo en el siguiente que
 * se agregue. Como la autoridad se guarda tal cual ("ROLE_ADMIN"), se compara
 * con hasAuthority y no con hasRole, que le antepondria otro "ROLE_".
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('" + Rol.ADMIN + "')")
@Tag(name = "Administracion", description = "Gestion de usuarios. No expone datos financieros de nadie")
public class AdministracionController {

    private final ServicioAdministracion servicio;

    public AdministracionController(ServicioAdministracion servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/usuarios")
    @Operation(summary = "Listar usuarios", description = "RF-023. Sin saldos ni movimientos (RN-005)")
    public ResponseEntity<List<UsuarioAdminResponse>> usuarios() {
        return ResponseEntity.ok(servicio.listarUsuarios());
    }

    @GetMapping("/resumen")
    @Operation(summary = "Conteos de la plataforma", description = "RF-023")
    public ResponseEntity<ResumenAdminResponse> resumen() {
        return ResponseEntity.ok(servicio.resumen());
    }

    @PatchMapping("/usuarios/{id}/desactivar")
    @Operation(summary = "Desactivar una cuenta", description = "RF-023. Queda registrado en auditoria")
    public ResponseEntity<UsuarioAdminResponse> desactivar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(servicio.desactivar(principal.getId(), id));
    }

    @PatchMapping("/usuarios/{id}/activar")
    @Operation(summary = "Reactivar una cuenta", description = "RF-023. Queda registrado en auditoria")
    public ResponseEntity<UsuarioAdminResponse> activar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(servicio.activar(principal.getId(), id));
    }

    @GetMapping("/auditoria")
    @Operation(summary = "Historial de acciones administrativas", description = "RF-024")
    public ResponseEntity<List<AuditoriaResponse>> auditoria(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(servicio.historial(page, size));
    }
}
