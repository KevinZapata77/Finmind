package com.finmind.auth.controller;

import com.finmind.auth.dto.AuthResponse;
import com.finmind.auth.dto.LoginRequest;
import com.finmind.auth.dto.RegistroRequest;
import com.finmind.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacion", description = "Registro de cuentas y emision de tokens de acceso")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    @Operation(summary = "Crear una cuenta de usuario",
            description = "Registra un usuario final y devuelve un token de acceso. "
                    + "El rol se asigna en el servidor, no se acepta del cliente.")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(peticion));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion",
            description = "Verifica las credenciales y devuelve un token JWT. "
                    + "Ante credenciales invalidas responde 401 con un mensaje generico.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest peticion) {
        return ResponseEntity.ok(authService.autenticar(peticion));
    }
}
