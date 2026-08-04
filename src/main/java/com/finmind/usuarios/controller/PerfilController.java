package com.finmind.usuarios.controller;

import com.finmind.auth.dto.UsuarioResponse;
import com.finmind.common.exception.RecursoNoEncontradoException;
import com.finmind.common.security.UsuarioPrincipal;
import com.finmind.usuarios.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Perfil del usuario autenticado")
public class PerfilController {

    private final UsuarioRepository usuarioRepository;

    public PerfilController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * El id se toma del token, nunca de la URL. Por eso no existe un
     * /api/v1/usuarios/{id}: no habria forma de impedir que alguien pida el 7.
     */
    @GetMapping("/me")
    @Operation(summary = "Datos del usuario autenticado",
            description = "El usuario se identifica por el token. Requiere Authorization: Bearer <token>.")
    public ResponseEntity<UsuarioResponse> miPerfil(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return usuarioRepository.findById(principal.getId())
                .map(UsuarioResponse::de)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }
}
