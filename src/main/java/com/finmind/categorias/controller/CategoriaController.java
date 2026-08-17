package com.finmind.categorias.controller;

import com.finmind.categorias.dto.*;
import com.finmind.categorias.service.ServicioCategorias;
import com.finmind.common.security.UsuarioPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Categorias (RF-009 a RF-011). */
@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorias", description = "Categorias del sistema y propias del usuario")
public class CategoriaController {

    private final ServicioCategorias servicio;

    public CategoriaController(ServicioCategorias servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listar las categorias disponibles", description = "RF-010. Incluye las del sistema")
    public ResponseEntity<List<CategoriaResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "true") boolean soloActivas) {
        return ResponseEntity.ok(servicio.listar(principal.getId(), tipo, soloActivas));
    }

    @PostMapping
    @Operation(summary = "Crear una categoria propia", description = "RF-009")
    public ResponseEntity<CategoriaResponse> crear(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CategoriaRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(principal.getId(), peticion));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar una categoria propia", description = "RF-011. El tipo no se edita")
    public ResponseEntity<CategoriaResponse> editar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody EditarCategoriaRequest peticion) {
        return ResponseEntity.ok(servicio.editar(principal.getId(), id, peticion));
    }

    @PatchMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar una categoria propia", description = "RF-011")
    public ResponseEntity<CategoriaResponse> desactivar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(servicio.desactivar(principal.getId(), id));
    }

    @PatchMapping("/{id}/activar")
    @Operation(summary = "Reactivar una categoria propia", description = "RF-011")
    public ResponseEntity<CategoriaResponse> activar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(servicio.activar(principal.getId(), id));
    }
}
