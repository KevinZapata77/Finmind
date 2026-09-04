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
import org.springframework.http.HttpHeaders;
import com.finmind.common.security.CookieDeSesion;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Verificacion y recuperacion",
     description = "Verificacion del correo y recuperacion de la contrasena mediante codigo")
@SecurityRequirements
public class IdentidadController {

    private final ServicioIdentidad servicio;
    private final CookieDeSesion cookieDeSesion;

    public IdentidadController(ServicioIdentidad servicio, CookieDeSesion cookieDeSesion) {
        this.servicio = servicio;
        this.cookieDeSesion = cookieDeSesion;
    }

    @PostMapping("/verificar")
    @Operation(summary = "Verificar el correo con el codigo recibido",
            description = "RF-025. Si el codigo es valido la cuenta queda verificada y se devuelve un token.")
    public ResponseEntity<AuthResponse> verificar(@Valid @RequestBody VerificarRequest peticion) {
        // SEG-08. Este endpoint deja al usuario autenticado, asi que tiene que
        // abrir la cookie igual que el login. Si no, verificar el correo dejaria
        // una sesion a medias: token en el cuerpo y ninguna cookie, o sea que la
        // siguiente peticion del navegador saldria sin autenticar.
        AuthResponse respuesta = servicio.verificar(peticion.correo(), peticion.codigo());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieDeSesion.crear(respuesta.token()).toString())
                .body(respuesta);
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
        // SEG-08. Misma razon que en verificar: aqui el usuario queda autenticado.
        AuthResponse respuesta = servicio.restablecer(
                peticion.correo(), peticion.codigo(), peticion.contrasenaNueva());
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookieDeSesion.crear(respuesta.token()).toString())
                .body(respuesta);
    }
}
