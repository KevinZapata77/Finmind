package com.finmind.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/api/v1/auth/registro",
            "/api/v1/auth/login",
            // RF-025 a RF-028: el usuario aun no tiene sesion cuando los usa
            "/api/v1/auth/verificar",
            "/api/v1/auth/reenviar-codigo",
            "/api/v1/auth/recuperar",
            "/api/v1/auth/restablecer",
            // RF-029 y RF-030: el usuario todavia no tiene sesion cuando pasa por aqui
            "/oauth2/**",
            "/login/oauth2/**",
            // OPS-03: permite verificar disponibilidad sin credenciales.
            // Solo se expone health; el resto de actuator queda cerrado.
            "/actuator/health"
    };

    /*
     * SEG-07. La documentacion de la API, en su propia lista y con interruptor.
     *
     * Estas tres rutas estaban mezcladas con las de autenticacion, abiertas en
     * todos los perfiles y sin manera de cerrarlas sin recompilar. /v3/api-docs
     * entrega el contrato completo: cada endpoint, cada parametro, cada forma de
     * respuesta. Para quien quiera atacar la API es el mapa, servido.
     *
     * En ESTE proyecto se dejan abiertas a proposito, y no es un descuido: el
     * profesor tiene que poder probar la API sin credenciales (DOC-04, API-08).
     * Cerrarlas por seguridad teorica costaria la evaluacion.
     *
     * Lo que cambia es que ahora la decision es explicita y reversible sin
     * tocar codigo: finmind.swagger.publico=false y quedan detras del token.
     * Un riesgo aceptado a conciencia y con el interruptor a la vista no es lo
     * mismo que un riesgo que nadie miro.
     */
    private static final String[] RUTAS_SWAGGER = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UsuarioDetallesService usuarioDetallesService;
    private final RespuestaNoAutorizada respuestaNoAutorizada;
    private final ManejadorExitoGoogle manejadorExitoGoogle;
    // Puede no existir: si no hay credenciales de Google configuradas, Spring
    // no crea este bean y el acceso con Google simplemente queda deshabilitado.
    private final ObjectProvider<ClientRegistrationRepository> registrosOAuth2;
    private final String origenesPermitidos;
    private final boolean swaggerPublico;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          UsuarioDetallesService usuarioDetallesService,
                          RespuestaNoAutorizada respuestaNoAutorizada,
                          ManejadorExitoGoogle manejadorExitoGoogle,
                          ObjectProvider<ClientRegistrationRepository> registrosOAuth2,
                          @Value("${finmind.cors.allowed-origins}") String origenesPermitidos,
                          @Value("${finmind.swagger.publico:true}") boolean swaggerPublico) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.usuarioDetallesService = usuarioDetallesService;
        this.respuestaNoAutorizada = respuestaNoAutorizada;
        this.manejadorExitoGoogle = manejadorExitoGoogle;
        this.registrosOAuth2 = registrosOAuth2;
        this.origenesPermitidos = origenesPermitidos;
        this.swaggerPublico = swaggerPublico;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // La API no usa cookies de sesion, el token va en el encabezado.
                // Sin sesiones no hay superficie para CSRF.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers(RUTAS_PUBLICAS).permitAll();

                    // SEG-07. Con finmind.swagger.publico=false, la documentacion
                    // pasa a exigir token como cualquier otro endpoint. No se
                    // borra la ruta: cae en anyRequest().authenticated().
                    if (swaggerPublico) {
                        auth.requestMatchers(RUTAS_SWAGGER).permitAll();
                    }

                    // Todo lo demas exige token. Por defecto se deniega:
                    // agregar un endpoint nuevo no lo deja abierto por descuido.
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respuestaNoAutorizada)
                        .accessDeniedHandler(respuestaNoAutorizada))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // El acceso con Google solo se activa si hay credenciales configuradas.
        // Asi la aplicacion arranca igual en un equipo sin esas claves.
        if (registrosOAuth2.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                    .successHandler(manejadorExitoGoogle)
                    .failureUrl("/api/v1/auth/login?error=google"));
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt genera y almacena la sal dentro del propio hash (SEG-01).
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider();
        proveedor.setUserDetailsService(usuarioDetallesService);
        proveedor.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(proveedor);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origenesPermitidos.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", config);
        return fuente;
    }
}
