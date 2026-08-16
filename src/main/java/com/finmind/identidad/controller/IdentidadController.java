package com.finmind.identidad.controller;

import com.finmind.auth.dto.AuthResponse;
import com.finmind.identidad.dto.*;
import com.finmind.identidad.service.ServicioIdentidad;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Verificacion y recuperacion",
     description = "Verificacion del correo y recuperacion de la contrasena mediante codigo")
@SecurityRequirements
public class IdentidadController {

    private final ServicioIdentidad servicio;

    public IdentidadController(ServicioIdentidad servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/verificar")
    @Operation(summary = "Verificar el correo con el codigo recibido",
            description = "RF-025. Si el codigo es valido la cuenta queda verificada y se devuelve un token.")
    public ResponseEntity<AuthResponse> verificar(@Valid @RequestBody VerificarRequest peticion) {
        return ResponseEntity.ok(servicio.verificar(peticion.correo(), peticion.codigo()));
    }

    @PostMapping("/reenviar-codigo")
    @Operation(summary = "Reenviar el codigo de verificacion",
            description = "RF-026. Responde siempre 202, exista o no el correo, para no revelar que cuentas estan registradas.")
    public ResponseEntity<MensajeResponse> reenviar(@Valid @RequestBody CorreoRequest peticion) {
        servicio.reenviarCodigo(peticion.correo());
        return ResponseEntity.accepted().body(new MensajeResponse(
                "Si esa cuenta existe y no esta verificada, enviamos un codigo nuevo."));
    }

    @PostMapping("/recuperar")
    @Operation(summary = "Solicitar el codigo para restablecer la contrasena",
            description = "RF-027. Regla RN-014: la respuesta es identica exista o no el correo registrado.")
    public ResponseEntity<MensajeResponse> recuperar(@Valid @RequestBody CorreoRequest peticion) {
        servicio.solicitarRecuperacion(peticion.correo());
        return ResponseEntity.accepted().body(new MensajeResponse(
                "Si ese correo esta registrado, te enviamos un codigo para restablecer la contrasena."));
    }

    @PostMapping("/restablecer")
    @Operation(summary = "Definir una contrasena nueva con el codigo",
            description = "RF-028. La contrasena anterior deja de servir y el usuario queda autenticado.")
    public ResponseEntity<AuthResponse> restablecer(@Valid @RequestBody RestablecerRequest peticion) {
        return ResponseEntity.status(HttpStatus.OK).body(
                servicio.restablecer(peticion.correo(), peticion.codigo(), peticion.contrasenaNueva()));
    }
}
