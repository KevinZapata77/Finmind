package com.finmind.identidad.service;

import com.finmind.auth.dto.AuthResponse;
import com.finmind.auth.dto.UsuarioResponse;
import com.finmind.common.security.JwtService;
import com.finmind.common.security.UsuarioPrincipal;
import com.finmind.identidad.entity.CodigoVerificacion;
import com.finmind.identidad.repository.CodigoVerificacionRepository;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ServicioIdentidad(UsuarioRepository usuarios,
                             CodigoVerificacionRepository codigos,
                             ServicioCodigos servicioCodigos,
                             ServicioCorreo correo,
                             PasswordEncoder encoder,
                             JwtService jwtService) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.servicioCodigos = servicioCodigos;
        this.correo = correo;
        this.encoder = encoder;
        this.jwtService = jwtService;
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
        usuarios.findByCorreo(correoRecibido.trim().toLowerCase())
                .filter(u -> !u.estaVerificado())
                .ifPresent(this::enviarCodigoVerificacion);
    }

    /** RF-027. RN-014: respuesta uniforme, exista o no el correo. */
    @Transactional
    public void solicitarRecuperacion(String correoRecibido) {
        Optional<Usuario> encontrado = usuarios.findByCorreo(correoRecibido.trim().toLowerCase());
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
