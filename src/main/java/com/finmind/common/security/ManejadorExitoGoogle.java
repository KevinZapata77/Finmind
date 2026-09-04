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
    private final String urlExito;
    private final String urlError;

    public ManejadorExitoGoogle(ServicioUsuarioGoogle servicioGoogle,
                                JwtService jwtService,
                                @Value("${finmind.oauth2.redireccion-exito}") String urlExito,
                                @Value("${finmind.oauth2.redireccion-error}") String urlError) {
        this.servicioGoogle = servicioGoogle;
        this.jwtService = jwtService;
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
             * SEG-06. El token vuelve en el FRAGMENTO de la URL, no en la cadena
             * de consulta.
             *
             * Antes iba como ?token=... El frontend lo borraba de la barra de
             * direcciones de inmediato, y eso resolvia el historial del
             * navegador, pero solo eso. Lo que quedaba abierto:
             *
             *   - La cadena de consulta viaja en la LINEA DE PETICION HTTP. La
             *     escribe en su registro cualquier cosa que este en el camino:
             *     el servidor, un proxy inverso, un balanceador, la consola de
             *     la plataforma donde se despliegue. Un token de sesion completo
             *     en texto plano dentro de un log es un token regalado, y los
             *     logs se guardan mucho mas tiempo que la vigencia del token.
             *
             *   - El encabezado Referer. Cualquier recurso que la pagina pida
             *     antes de que corra el replaceState se lleva la URL entera —con
             *     el token— hacia un tercero.
             *
             * El fragmento no tiene ninguno de los dos problemas: el navegador
             * NUNCA lo manda al servidor. Se queda del lado del cliente, que es
             * justo donde tiene que quedarse.
             *
             * No hace falta codificar: un JWT es base64url y un punto, todos
             * caracteres validos en un fragmento.
             */
            String destino = UriComponentsBuilder.fromUriString(urlExito)
                    .fragment("token=" + token)
                    .build().toUriString();

            respuesta.sendRedirect(destino);

        } catch (ServicioUsuarioGoogle.CuentaGoogleException ex) {
            log.warn("Acceso con Google rechazado: {}", ex.getMessage());
            respuesta.sendRedirect(urlError + "?error="
                    + URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8));
        }
    }
}
