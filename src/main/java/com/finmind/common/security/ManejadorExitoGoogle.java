package com.finmind.common.security;

import com.finmind.identidad.service.ServicioUsuarioGoogle;
import com.finmind.usuarios.entity.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Que ocurre cuando Google confirma la identidad del usuario.
 *
 * La API es sin estado: no se crea sesion de servidor. Se traduce el perfil
 * de Google a un usuario de FinMind, se emite NUESTRO token, y se devuelve
 * el navegador al frontend con ese token en la URL.
 */
@Component
public class ManejadorExitoGoogle implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorExitoGoogle.class);

    private final ServicioUsuarioGoogle servicioGoogle;
    private final JwtService jwtService;
    private final CookieDeSesion cookieDeSesion;
    private final String urlExito;
    private final String urlError;

    public ManejadorExitoGoogle(ServicioUsuarioGoogle servicioGoogle,
                                JwtService jwtService,
                                CookieDeSesion cookieDeSesion,
                                @Value("${finmind.oauth2.redireccion-exito}") String urlExito,
                                @Value("${finmind.oauth2.redireccion-error}") String urlError) {
        this.servicioGoogle = servicioGoogle;
        this.jwtService = jwtService;
        this.cookieDeSesion = cookieDeSesion;
        this.urlExito = urlExito;
        this.urlError = urlError;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest peticion,
                                        HttpServletResponse respuesta,
                                        Authentication autenticacion) throws IOException {
        try {
            OAuth2User perfil = (OAuth2User) autenticacion.getPrincipal();
            Usuario usuario = servicioGoogle.obtenerOCrear(perfil);

            String token = jwtService.generarToken(new UsuarioPrincipal(usuario));

            /*
             * SEG-08. La sesion se abre en la cookie HttpOnly, igual que en el
             * login normal. Es lo que permite que el token NO tenga que viajar
             * en la URL de vuelta.
             */
            respuesta.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE,
                    cookieDeSesion.crear(token).toString());

            /*
             * SEG-06 y SEG-08. El token NO viaja en la URL. Ni en la cadena de
             * consulta ni en el fragmento: no viaja.
             *
             * Este endpoint recorrio las tres versiones y vale la pena que quede
             * escrito, porque explica por que la cookie es la respuesta buena:
             *
             *   1. ?token=... El frontend lo borraba de la barra de direcciones
             *      enseguida, lo que resolvia el historial del navegador y solo
             *      eso. La cadena de consulta viaja en la LINEA DE PETICION
             *      HTTP, asi que la escribe en su registro cualquier cosa del
             *      camino —servidor, proxy inverso, balanceador, la consola de
             *      la plataforma— y ademas se filtra por el encabezado Referer.
             *      Un token de sesion en texto plano dentro de un log es un
             *      token regalado, y los logs viven mucho mas que el token.
             *
             *   2. #token=... Mejor: el navegador nunca manda el fragmento al
             *      servidor, asi que desaparece el problema de los registros y
             *      del Referer. Pero el token seguia pasando por la barra de
             *      direcciones, a la vista y en el historial.
             *
             *   3. La cookie, que es esto. El secreto va en un encabezado
             *      Set-Cookie, no en la URL, y con HttpOnly ni el propio
             *      JavaScript de la pagina lo puede leer. La URL de retorno
             *      queda limpia: no lleva nada.
             *
             * La variable token se sigue usando arriba, para crear la cookie.
             */
            respuesta.sendRedirect(UriComponentsBuilder.fromUriString(urlExito)
                    .build().toUriString());

        } catch (ServicioUsuarioGoogle.CuentaGoogleException ex) {
            log.warn("Acceso con Google rechazado: {}", ex.getMessage());
            respuesta.sendRedirect(urlError + "?error="
                    + URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8));
        }
    }
}
