package com.finmind.common.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
 * Lee el encabezado Authorization: Bearer <token> y, si el token es valido,
 * establece la autenticacion para esa peticion.
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

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String encabezado = request.getHeader(ENCABEZADO);

        if (encabezado == null || !encabezado.startsWith(PREFIJO)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = encabezado.substring(PREFIJO.length());

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
