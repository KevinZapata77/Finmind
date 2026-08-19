package com.finmind.auth.service;

import com.finmind.auth.dto.AuthResponse;
import com.finmind.auth.dto.LoginRequest;
import com.finmind.auth.dto.RegistroRequest;
import com.finmind.auth.dto.UsuarioResponse;
import com.finmind.common.exception.CorreoYaRegistradoException;
import com.finmind.common.security.JwtService;
import com.finmind.common.security.LimitadorDeIntentos;
import com.finmind.common.security.UsuarioPrincipal;
import com.finmind.cuentas.entity.Cuenta;
import com.finmind.cuentas.repository.CuentaRepository;
import com.finmind.identidad.service.ServicioCaptcha;
import com.finmind.identidad.service.ServicioIdentidad;
import com.finmind.usuarios.entity.Rol;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.RolRepository;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ServicioIdentidad servicioIdentidad;
    private final ServicioCaptcha servicioCaptcha;
    private final CuentaRepository cuentas;
    private final LimitadorDeIntentos limitador;
    private final int maxPorCorreo;
    private final int maxPorIp;
    private final Duration ventana;

    public AuthService(UsuarioRepository usuarioRepository,
                       RolRepository rolRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       ServicioIdentidad servicioIdentidad,
                       ServicioCaptcha servicioCaptcha,
                       CuentaRepository cuentas,
                       LimitadorDeIntentos limitador,
                       @Value("${finmind.login.max-por-correo:5}") int maxPorCorreo,
                       @Value("${finmind.login.max-por-ip:20}") int maxPorIp,
                       @Value("${finmind.login.ventana-minutos:15}") int ventanaMinutos) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.servicioIdentidad = servicioIdentidad;
        this.servicioCaptcha = servicioCaptcha;
        this.cuentas = cuentas;
        this.limitador = limitador;
        this.maxPorCorreo = maxPorCorreo;
        this.maxPorIp = maxPorIp;
        this.ventana = Duration.ofMinutes(ventanaMinutos);
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

        // Toda cuenta nueva arranca con una cuenta de efectivo.
        //
        // POR QUE: un movimiento exige cuenta por llave foranea. Sin esto, el
        // primer gesto de un usuario recien registrado no era anotar su plata
        // sino llenar un formulario de cuentas que no habia pedido. El tramite
        // iba antes del valor, y eso es lo que hace que una aplicacion se
        // abandone a los tres dias.
        cuentas.save(new Cuenta(guardado, "Efectivo", Cuenta.EFECTIVO,
                java.math.BigDecimal.ZERO, "COP"));

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
     *
     * SEG-03: hay un tope de intentos fallidos. Se cuenta por dos claves a la vez:
     *
     *   por correo  frena probar mil contrasenas contra UNA cuenta
     *   por IP      frena probar una contrasena contra MIL cuentas, que es lo
     *               que hace quien reutiliza una lista de credenciales filtradas
     *
     * Cada intento se cuenta antes de comprobar la contrasena, y al entrar bien
     * el contador se borra. El efecto practico es que solo pesan los fallos:
     * quien se equivoco dos veces y despues acerto no arrastra nada.
     *
     * Se cuenta ANTES a proposito. Contar solo despues de saber que fallo
     * obligaria a distinguir "credencial mala" de "cuenta inexistente" para
     * decidir si contar, y esa distincion es justo lo que no debe existir.
     *
     * @param ip direccion de quien pide. La resuelve el controlador, porque el
     *           servicio no debe conocer HttpServletRequest.
     */
    @Transactional
    public AuthResponse autenticar(LoginRequest peticion, String ip) {
        String correo = peticion.correo().trim().toLowerCase();
        String clavePorCorreo = "login:" + correo;
        String clavePorIp = "login-ip:" + ip;

        // El mensaje es el mismo en los dos casos y no dice si la cuenta existe.
        String aviso = "Demasiados intentos fallidos. Espera unos minutos antes de volver a intentar.";
        limitador.comprobar(clavePorIp, maxPorIp, ventana, aviso);
        limitador.comprobar(clavePorCorreo, maxPorCorreo, ventana, aviso);

        // El intento ya quedo contado. Si las credenciales fallan, la excepcion
        // sube tal cual y el manejador responde 401 como siempre: el limitador
        // no cambia ninguna respuesta que ya estuviera probada.
        Authentication autenticacion = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(correo, peticion.contrasena()));

        UsuarioPrincipal principal = (UsuarioPrincipal) autenticacion.getPrincipal();

        Usuario usuario = usuarioRepository.findByCorreo(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
        usuario.registrarAcceso();

        // Entro bien: se olvidan los fallos previos de esa cuenta y esa IP.
        limitador.olvidar(clavePorCorreo);
        limitador.olvidar(clavePorIp);

        return AuthResponse.de(
                jwtService.generarToken(principal),
                jwtService.getExpiracionMs(),
                UsuarioResponse.de(usuario));
    }
}
