package com.finmind.auth.controller;

import com.finmind.auth.dto.AuthResponse;
import com.finmind.auth.dto.LoginRequest;
import com.finmind.auth.dto.RegistroRequest;
import com.finmind.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.finmind.common.security.CookieDeSesion;
import org.springframework.http.HttpHeaders;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacion", description = "Registro de cuentas y emision de tokens de acceso")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;
    private final CookieDeSesion cookieDeSesion;

    public AuthController(AuthService authService, CookieDeSesion cookieDeSesion) {
        this.authService = authService;
        this.cookieDeSesion = cookieDeSesion;
    }

    @PostMapping("/registro")
    @Operation(summary = "Crear una cuenta de usuario",
            description = "Registra un usuario final y devuelve un token de acceso. "
                    + "El rol se asigna en el servidor, no se acepta del cliente. "
                    + "Ademas abre la sesion en una cookie HttpOnly (SEG-08).")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest peticion) {
        AuthResponse respuesta = authService.registrar(peticion);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookieDeSesion.crear(respuesta.token()).toString())
                .body(respuesta);
    }

    /*
      SEG-08. El login abre la sesion en una cookie HttpOnly Y SIGUE devolviendo
      el token en el cuerpo.

      Devolver los dos parece redundante y no lo es:

        - El navegador usa la cookie. Es el punto de todo: ningun script puede
          leerla, asi que un XSS no se lleva la sesion.

        - El token en el cuerpo es lo que mantiene vivas las 189 pruebas, la
          consola de Swagger y cualquier cliente que no sea un navegador. Sin
          el, este cambio obligaria a reescribir las pruebas de autenticacion
          justo antes del congelamiento.

      Lo que NO hay que hacer es que el frontend siga guardando ese token en
      sessionStorage: ahi volveria el problema que se vino a resolver. El
      frontend lo ignora y se apoya solo en la cookie.
    */
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion",
            description = "Verifica las credenciales, abre la sesion en una cookie HttpOnly "
                    + "y devuelve el token JWT para clientes que no son navegador. "
                    + "Ante credenciales invalidas responde 401 con un mensaje generico. "
                    + "Tras varios intentos fallidos responde 429 con el encabezado Retry-After.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest peticion,
                                              HttpServletRequest http) {
        AuthResponse respuesta = authService.autenticar(peticion, ipDe(http));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieDeSesion.crear(respuesta.token()).toString())
                .body(respuesta);
    }

    /**
     * SEG-08. Cerrar sesion de verdad.
     *
     * Antes no existia este endpoint y no hacia falta: el frontend borraba el
     * token de sessionStorage y con eso la sesion desaparecia del navegador.
     * Con una cookie HttpOnly eso ya no es posible —ningun script la puede
     * borrar—, asi que quien la puso tiene que quitarla.
     *
     * Es publico a proposito. Cerrar sesion con un token ya vencido tiene que
     * funcionar igual: si exigiera estar autenticado, el usuario con la sesion
     * expirada se quedaria con la cookie muerta pegada en el navegador y sin
     * forma de limpiarla.
     *
     * No invalida el token en el servidor, y esto hay que decirlo claro: un JWT
     * es valido hasta que expira, no hay lista de revocados. Si alguien copio
     * el token antes, le sigue sirviendo hasta que venza. Cerrar sesion aqui
     * significa "este navegador se olvida de la sesion", no "el token muere".
     * Revocar de verdad pediria guardar estado en el servidor, que es
     * justamente lo que una API sin estado no hace.
     */
    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesion",
            description = "Borra la cookie de sesion del navegador. No invalida el token "
                    + "en el servidor: un JWT vive hasta que expira.")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieDeSesion.borrar().toString())
                .build();
    }

    /**
     * Direccion de quien hace la peticion, para el tope por IP.
     *
     * Se usa getRemoteAddr y NO el encabezado X-Forwarded-For: ese lo escribe el
     * cliente y se puede inventar, asi que confiar en el permitiria saltarse el
     * limite cambiandolo en cada intento. Si algun dia la aplicacion queda detras
     * de un proxy, hay que configurar Spring para que lo interprete solo cuando
     * venga de ese proxy, no aceptarlo de cualquiera.
     */
    private String ipDe(HttpServletRequest http) {
        String ip = http.getRemoteAddr();
        return (ip == null || ip.isBlank()) ? "desconocida" : ip;
    }
}
