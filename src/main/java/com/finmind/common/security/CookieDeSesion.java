package com.finmind.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * SEG-08. La sesion en una cookie HttpOnly.
 *
 * POR QUE SE AGREGA, SI YA HABIA TOKEN
 * El token se guardaba en sessionStorage, que JavaScript puede leer. Si algun
 * dia entra un XSS en la aplicacion —una dependencia comprometida, un texto de
 * usuario que se pinta sin escapar—, ese script se lleva la sesion completa y
 * no hay nada que lo impida. Una cookie con HttpOnly no la puede leer ningun
 * script: solo la manda el navegador, y solo al servidor que la puso.
 *
 * ESTO NO ES "MAS SEGURO" A SECAS: ES UN INTERCAMBIO
 * Al pasar a cookie se gana contra XSS y se PIERDE contra CSRF, porque la
 * cookie viaja sola en cada peticion y un sitio ajeno puede provocarla. Hay que
 * taparlo, y aqui se tapa con SameSite. Ver la nota de SameSite mas abajo, que
 * explica por que en esta API alcanza.
 *
 * POR QUE ES ADITIVO Y NO UN REEMPLAZO
 * El filtro lee primero la cookie y, si no esta, sigue leyendo el encabezado
 * Authorization. Las dos puertas quedan abiertas a proposito:
 *
 *   - El navegador usa la cookie, que es lo que se queria.
 *   - Las 189 pruebas y Swagger usan el encabezado y no hay que tocarlas. Un
 *     cambio de autenticacion a cuatro dias del congelamiento que ademas
 *     obligue a reescribir las pruebas es como se rompe un proyecto.
 *   - Cualquier cliente que no sea un navegador —curl, Postman, el evaluador
 *     probando la API— sigue funcionando igual.
 */
@Component
public class CookieDeSesion {

    /** El nombre no dice "jwt" ni "token": no hace falta anunciar que hay dentro. */
    public static final String NOMBRE = "finmind_sesion";

    private final boolean segura;
    private final String mismositio;
    private final long vigenciaMs;

    public CookieDeSesion(@Value("${finmind.cookie.segura:false}") boolean segura,
                          @Value("${finmind.cookie.mismositio:Lax}") String mismositio,
                          @Value("${finmind.jwt.expiration-ms}") long vigenciaMs) {
        this.segura = segura;
        this.mismositio = mismositio;
        this.vigenciaMs = vigenciaMs;
    }

    /**
     * La cookie con la sesion abierta.
     *
     * httpOnly: ningun script la puede leer. Es el motivo de todo esto.
     *
     * secure: solo viaja por HTTPS. Va en false por defecto porque en local se
     * trabaja en http://localhost y con secure en true el navegador NO manda la
     * cookie, o sea que nadie podria iniciar sesion en desarrollo. En un
     * despliegue real se pone finmind.cookie.segura=true, y es obligatorio: sin
     * eso la cookie viaja en texto plano y cualquiera en la misma red la copia.
     *
     * sameSite Lax: el navegador NO manda la cookie en peticiones que vengan de
     * otro sitio, salvo en una navegacion de primer nivel por GET. Eso es lo
     * que frena el CSRF aqui, y alcanza por una razon concreta: en esta API
     * TODOS los endpoints que cambian datos son POST, PUT, PATCH o DELETE, y en
     * ninguno de esos casos Lax manda la cookie. Lo unico que puede llevarla es
     * un GET, y los GET no modifican nada; la respuesta tampoco se puede leer
     * desde otro origen porque CORS lo impide.
     *
     * Por eso no se agrega token anti-CSRF: seria una segunda cerradura para
     * una puerta que ya no abre. Si algun dia un GET cambiara datos —lo que
     * seria un error de diseno aparte— habria que ponerlo.
     *
     * No se usa Strict aunque suene mejor: con Strict el navegador tampoco
     * manda la cookie al volver de Google, y el acceso con Google quedaria
     * roto.
     *
     * path /: la manda en toda la API, no solo bajo /api.
     *
     * maxAge igual a la vigencia del token: que la cookie sobreviva al token
     * solo consigue que el usuario parezca conectado y reciba 401 en todo.
     */
    public ResponseCookie crear(String token) {
        return ResponseCookie.from(NOMBRE, token)
                .httpOnly(true)
                .secure(segura)
                .sameSite(mismositio)
                .path("/")
                .maxAge(Duration.ofMillis(vigenciaMs))
                .build();
    }

    /**
     * La cookie que borra la sesion.
     *
     * Se manda la misma cookie vacia y con maxAge en cero. Tiene que llevar
     * EXACTAMENTE los mismos path, secure y sameSite que la original: si alguno
     * cambia, el navegador la trata como otra cookie distinta, crea esa y deja
     * la de la sesion intacta. Es el error clasico del cierre de sesion que no
     * cierra nada.
     */
    public ResponseCookie borrar() {
        return ResponseCookie.from(NOMBRE, "")
                .httpOnly(true)
                .secure(segura)
                .sameSite(mismositio)
                .path("/")
                .maxAge(0)
                .build();
    }
}
