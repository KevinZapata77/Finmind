package com.finmind.auth.service;

import com.finmind.auth.dto.AuthResponse;
import com.finmind.auth.dto.LoginRequest;
import com.finmind.auth.dto.RegistroRequest;
import com.finmind.auth.dto.UsuarioResponse;
import com.finmind.common.exception.CorreoYaRegistradoException;
import com.finmind.common.security.JwtService;
import com.finmind.common.security.UsuarioPrincipal;
import com.finmind.identidad.service.ServicioCaptcha;
import com.finmind.identidad.service.ServicioIdentidad;
import com.finmind.usuarios.entity.Rol;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.RolRepository;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ServicioIdentidad servicioIdentidad;
    private final ServicioCaptcha servicioCaptcha;

    public AuthService(UsuarioRepository usuarioRepository,
                       RolRepository rolRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       ServicioIdentidad servicioIdentidad,
                       ServicioCaptcha servicioCaptcha) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.servicioIdentidad = servicioIdentidad;
        this.servicioCaptcha = servicioCaptcha;
    }

    /**
     * Crea una cuenta de usuario final.
     *
     * El rol se asigna aqui, en el servidor. No se acepta del cliente: si el rol
     * viniera en la peticion, cualquiera podria registrarse como administrador.
     */
    @Transactional
    public AuthResponse registrar(RegistroRequest peticion) {
        // Primero el CAPTCHA: si no es una persona, no se toca la base de datos
        servicioCaptcha.verificar(peticion.captchaToken());

        String correo = peticion.correo().trim().toLowerCase();

        if (usuarioRepository.existsByCorreo(correo)) {
            throw new CorreoYaRegistradoException("Ya existe una cuenta registrada con ese correo");
        }

        Rol rolUsuario = rolRepository.findByNombre(Rol.USUARIO)
                .orElseThrow(() -> new IllegalStateException(
                        "El rol " + Rol.USUARIO + " no existe. Verifica que la migracion V1 se aplico."));

        Usuario usuario = new Usuario(
                peticion.nombre().trim(),
                peticion.apellido().trim(),
                correo,
                passwordEncoder.encode(peticion.contrasena()),
                rolUsuario);

        Usuario guardado = usuarioRepository.save(usuario);

        // RN-011: la cuenta nace sin verificar y no puede iniciar sesion todavia.
        // Se emite y envia el codigo; el usuario continua en la pantalla UI-010.
        servicioIdentidad.enviarCodigoVerificacion(guardado);

        // No se emite token: el acceso llega tras verificar el correo.
        return AuthResponse.de(null, 0, UsuarioResponse.de(guardado));
    }

    /**
     * Verifica credenciales y emite el token.
     *
     * Si fallan, AuthenticationManager lanza BadCredentialsException y el
     * GlobalExceptionHandler responde 401 con un mensaje generico: nunca se
     * distingue entre "el correo no existe" y "la contrasena esta mal".
     */
    @Transactional
    public AuthResponse autenticar(LoginRequest peticion) {
        String correo = peticion.correo().trim().toLowerCase();

        Authentication autenticacion = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(correo, peticion.contrasena()));

        UsuarioPrincipal principal = (UsuarioPrincipal) autenticacion.getPrincipal();

        Usuario usuario = usuarioRepository.findByCorreo(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
        usuario.registrarAcceso();

        return AuthResponse.de(
                jwtService.generarToken(principal),
                jwtService.getExpiracionMs(),
                UsuarioResponse.de(usuario));
    }
}
