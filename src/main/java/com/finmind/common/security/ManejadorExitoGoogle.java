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

            // El token viaja en la URL de retorno. El frontend lo guarda y limpia
            // la barra de direcciones de inmediato para que no quede en el historial.
            String destino = UriComponentsBuilder.fromUriString(urlExito)
                    .queryParam("token", token)
                    .build().toUriString();

            respuesta.sendRedirect(destino);

        } catch (ServicioUsuarioGoogle.CuentaGoogleException ex) {
            log.warn("Acceso con Google rechazado: {}", ex.getMessage());
            respuesta.sendRedirect(urlError + "?error="
                    + URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8));
        }
    }
}
