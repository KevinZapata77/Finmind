package com.finmind.metas.controller;

import com.finmind.common.security.UsuarioPrincipal;
import com.finmind.metas.dto.*;
import com.finmind.metas.service.ServicioMetas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Metas de ahorro (RF-032 a RF-034). */
@RestController
@RequestMapping("/api/v1/metas")
@Tag(name = "Metas de ahorro", description = "Objetivos de ahorro del usuario y sus abonos")
public class MetaController {

    private final ServicioMetas servicio;

    public MetaController(ServicioMetas servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @Operation(summary = "Crear una meta", description = "RF-032")
    public ResponseEntity<MetaResponse> crear(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody MetaRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(principal.getId(), peticion));
    }

    @GetMapping
    @Operation(summary = "Listar las metas propias", description = "RF-034")
    public ResponseEntity<List<MetaResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(servicio.listar(principal.getId(), estado));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una meta", description = "RF-034")
    public ResponseEntity<MetaResponse> consultar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(servicio.consultar(principal.getId(), id));
    }

    @PostMapping("/{id}/abonos")
    @Operation(summary = "Abonar a una meta", description = "RF-033. Al llegar al objetivo se completa sola")
    public ResponseEntity<MetaResponse> abonar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody AbonoRequest peticion) {
        return ResponseEntity.ok(servicio.abonar(principal.getId(), id, peticion));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar una meta", description = "RF-034")
    public ResponseEntity<MetaResponse> editar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody MetaRequest peticion) {
        return ResponseEntity.ok(servicio.editar(principal.getId(), id, peticion));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar una meta", description = "RF-034. Conserva lo ahorrado")
    public ResponseEntity<MetaResponse> cancelar(
            @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(servicio.cancelar(principal.getId(), id));
    }
}
