package com.finmind.common.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * El middleware de autenticacion: se ejecuta antes de cualquier controlador,
 * saca el token de la peticion y, si es valido, deja al usuario autenticado
 * para esa peticion.
 *
 * SEG-08. Dos puertas de entrada, y en este orden:
 *
 *   1. La cookie HttpOnly finmind_sesion, que es la que usa el navegador.
 *   2. El encabezado Authorization: Bearer, que es la que usan las pruebas,
 *      Swagger, curl y Postman.
 *
 * POR QUE LA COOKIE VA PRIMERO
 * Si las dos vienen en la misma peticion hay que elegir una, y se elige la que
 * el usuario no puede manipular desde la pagina. El encabezado lo pone
 * JavaScript; la cookie la pone y la manda el navegador. Ante un token viejo
 * arrastrado en sessionStorage y una cookie recien emitida, la cookie es la
 * fuente de verdad.
 *
 * POR QUE NO SE QUITA EL ENCABEZADO
 * Porque es lo que hace que este cambio no rompa nada: 189 pruebas, la consola
 * de Swagger y cualquier cliente que no sea un navegador siguen entrando por
 * ahi. Cambiar la autenticacion a cuatro dias del congelamiento y ademas tener
 * que reescribir las pruebas es como se rompe un proyecto.
 *
 * Si el token falta o es invalido no lanza error aqui: deja la peticion sin
 * autenticar y es SecurityConfig quien decide si el recurso la exige. Eso permite
 * que los endpoints publicos sigan funcionando.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ENCABEZADO = "Authorization";
    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioDetallesService usuarioDetallesService;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioDetallesService usuarioDetallesService) {
        this.jwtService = jwtService;
        this.usuarioDetallesService = usuarioDetallesService;
    }

    /**
     * De donde sale el token. Null si no viene por ninguna de las dos vias.
     */
    private String extraerToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie galleta : request.getCookies()) {
                if (CookieDeSesion.NOMBRE.equals(galleta.getName())
                        && galleta.getValue() != null && !galleta.getValue().isBlank()) {
                    return galleta.getValue();
                }
            }
        }

        String encabezado = request.getHeader(ENCABEZADO);
        if (encabezado != null && encabezado.startsWith(PREFIJO)) {
            String delEncabezado = encabezado.substring(PREFIJO.length());
            // Un "Bearer " con nada detras no es un token: mejor null que una
            // cadena vacia que despues revienta al parsearse.
            return delEncabezado.isBlank() ? null : delEncabezado;
        }

        return null;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extraerToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String correo = jwtService.extraerCorreo(token);

            if (correo != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails usuario = usuarioDetallesService.loadUserByUsername(correo);

                if (jwtService.esValido(token, usuario) && usuario.isEnabled()) {
                    UsernamePasswordAuthenticationToken autenticacion =
                            new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                    autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                }
            }
        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException ex) {
            // Token malformado, expirado o de un usuario que ya no existe.
            // Se ignora silenciosamente: la peticion sigue sin autenticar.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
