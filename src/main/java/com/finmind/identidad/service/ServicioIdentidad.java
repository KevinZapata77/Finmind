package com.finmind.identidad.service;

import com.finmind.auth.dto.AuthResponse;
import com.finmind.auth.dto.UsuarioResponse;
import com.finmind.common.security.JwtService;
import com.finmind.common.security.LimitadorDeIntentos;
import com.finmind.common.security.UsuarioPrincipal;
import com.finmind.identidad.entity.CodigoVerificacion;
import com.finmind.identidad.repository.CodigoVerificacionRepository;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

/**
 * Flujos de verificacion de correo y recuperacion de contrasena.
 *
 * Principio que atraviesa toda la clase: ninguna respuesta revela si un
 * correo esta registrado (RN-014). Distinguirlo permitiria averiguar que
 * cuentas existen probando direcciones.
 */
@Service
public class ServicioIdentidad {

    private final UsuarioRepository usuarios;
    private final CodigoVerificacionRepository codigos;
    private final ServicioCodigos servicioCodigos;
    private final ServicioCorreo correo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final LimitadorDeIntentos limitador;
    private final int maxEnvios;
    private final Duration ventanaEnvios;
    private final Duration intervaloMinimo;

    public ServicioIdentidad(UsuarioRepository usuarios,
                             CodigoVerificacionRepository codigos,
                             ServicioCodigos servicioCodigos,
                             ServicioCorreo correo,
                             PasswordEncoder encoder,
                             JwtService jwtService,
                             LimitadorDeIntentos limitador,
                             @Value("${finmind.codigo.max-envios:5}") int maxEnvios,
                             @Value("${finmind.codigo.ventana-envios-minutos:60}") int ventanaMinutos,
                             @Value("${finmind.codigo.intervalo-minimo-segundos:60}") int intervaloSegundos) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.servicioCodigos = servicioCodigos;
        this.correo = correo;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.limitador = limitador;
        this.maxEnvios = maxEnvios;
        this.ventanaEnvios = Duration.ofMinutes(ventanaMinutos);
        this.intervaloMinimo = Duration.ofSeconds(intervaloSegundos);
    }

    /**
     * Tope de emision de codigos (SEG-04 y SEG-05).
     *
     * Se aplica ANTES de buscar el usuario y con el correo tal como llego, para
     * no romper RN-014: si el limite se comprobara despues de saber si la cuenta
     * existe, la diferencia de respuesta delataria cuales estan registradas.
     *
     * Dos controles a la vez:
     *   intervalo  dos envios seguidos tienen que estar separados
     *   tope       cuantos se admiten por hora
     *
     * Es lo que le devuelve sentido al limite de cinco intentos por codigo: con
     * cinco codigos por hora quedan 25 combinaciones de un millon, en lugar de
     * las infinitas que habia cuando el reenvio no tenia freno.
     */
    private void comprobarLimiteDeEnvios(String correoNormalizado) {
        // El aviso no dice si la cuenta existe. Sirve igual para el dueno que
        // pide otro codigo demasiado rapido que para quien esta abusando.
        String aviso = "Ya se envio un codigo hace poco. Revisa tu correo y espera un momento "
                + "antes de pedir otro.";
        limitador.comprobarIntervalo("codigo:" + correoNormalizado, intervaloMinimo, aviso);
        limitador.comprobar("codigo-hora:" + correoNormalizado, maxEnvios, ventanaEnvios,
                "Se alcanzo el maximo de codigos por hora para este correo. Intenta mas tarde.");
    }

    /** Emite y envia el codigo de verificacion. Se llama al registrarse. */
    @Transactional
    public void enviarCodigoVerificacion(Usuario usuario) {
        String codigo = servicioCodigos.emitir(usuario, CodigoVerificacion.VERIFICACION);
        correo.enviarCodigoVerificacion(usuario.getCorreo(), usuario.getNombre(),
                codigo, servicioCodigos.getVigenciaMinutos());
    }

    /** RF-025. Devuelve token: tras verificar, el usuario entra sin volver a autenticarse. */
    @Transactional
    public AuthResponse verificar(String correoRecibido, String codigoRecibido) {
        String correoNormalizado = correoRecibido.trim().toLowerCase();
        Usuario usuario = usuarios.findByCorreo(correoNormalizado)
                .orElseThrow(() -> new CodigoInvalidoException(
                        "El codigo no es valido o ya vencio. Solicita uno nuevo."));

        if (usuario.estaVerificado()) {
            throw new CodigoInvalidoException("Esta cuenta ya esta verificada. Puedes iniciar sesion.");
        }

        ServicioCodigos.Resultado r = servicioCodigos.validarYConsumir(
                usuario.getId(), CodigoVerificacion.VERIFICACION, codigoRecibido);

        if (r != ServicioCodigos.Resultado.VALIDO) throw traducir(r);

        usuario.marcarCorreoVerificado();
        usuario.registrarAcceso();

        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        return AuthResponse.de(jwtService.generarToken(principal),
                jwtService.getExpiracionMs(), UsuarioResponse.de(usuario));
    }

    /** RF-026. Responde igual exista o no el correo, y este o no verificado. */
    @Transactional
    public void reenviarCodigo(String correoRecibido) {
        String correoNormalizado = correoRecibido.trim().toLowerCase();
        comprobarLimiteDeEnvios(correoNormalizado);

        usuarios.findByCorreo(correoNormalizado)
                .filter(u -> !u.estaVerificado())
                .ifPresent(this::enviarCodigoVerificacion);
    }

    /** RF-027. RN-014: respuesta uniforme, exista o no el correo. */
    @Transactional
    public void solicitarRecuperacion(String correoRecibido) {
        String correoNormalizado = correoRecibido.trim().toLowerCase();
        comprobarLimiteDeEnvios(correoNormalizado);

        Optional<Usuario> encontrado = usuarios.findByCorreo(correoNormalizado);
        if (encontrado.isEmpty()) return;

        Usuario usuario = encontrado.get();
        // Quien entra con Google no tiene contrasena en FinMind que recuperar.
        // Se responde igual que en cualquier otro caso para no revelar nada.
        if (!usuario.esLocal()) return;

        String codigo = servicioCodigos.emitir(usuario, CodigoVerificacion.RECUPERACION);
        correo.enviarCodigoRecuperacion(usuario.getCorreo(), usuario.getNombre(),
                codigo, servicioCodigos.getVigenciaMinutos());
    }

    /** RF-028. Cambia la contrasena y deja al usuario autenticado. */
    @Transactional
    public AuthResponse restablecer(String correoRecibido, String codigoRecibido, String contrasenaNueva) {
        Usuario usuario = usuarios.findByCorreo(correoRecibido.trim().toLowerCase())
                .orElseThrow(() -> new CodigoInvalidoException(
                        "El codigo no es valido o ya vencio. Solicita uno nuevo."));

        ServicioCodigos.Resultado r = servicioCodigos.validarYConsumir(
                usuario.getId(), CodigoVerificacion.RECUPERACION, codigoRecibido);

        if (r != ServicioCodigos.Resultado.VALIDO) throw traducir(r);

        usuario.setContrasenaHash(encoder.encode(contrasenaNueva));
        // Quien recupera el acceso demuestra control del correo: queda verificado.
        usuario.marcarCorreoVerificado();
        usuario.registrarAcceso();

        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        return AuthResponse.de(jwtService.generarToken(principal),
                jwtService.getExpiracionMs(), UsuarioResponse.de(usuario));
    }

    private CodigoInvalidoException traducir(ServicioCodigos.Resultado r) {
        return switch (r) {
            case BLOQUEADO -> new CodigoInvalidoException(
                    "Superaste el numero de intentos. Solicita un codigo nuevo.");
            default -> new CodigoInvalidoException(
                    "El codigo no es valido o ya vencio. Solicita uno nuevo.");
        };
    }

    /** 400: el codigo no sirve. El mensaje nunca revela por que exactamente. */
    public static class CodigoInvalidoException extends RuntimeException {
        public CodigoInvalidoException(String mensaje) { super(mensaje); }
    }
}
