package com.finmind.identidad.service;

import com.finmind.usuarios.entity.Rol;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.RolRepository;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Traduce el perfil que devuelve Google a un usuario de FinMind.
 *
 * Tres situaciones posibles:
 *  1. Ya existe una cuenta de Google con ese identificador -> se reutiliza
 *  2. Existe una cuenta LOCAL con ese mismo correo         -> no se mezclan
 *  3. No existe nada                                       -> se crea, ya verificada
 */
@Service
public class ServicioUsuarioGoogle {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;

    public ServicioUsuarioGoogle(UsuarioRepository usuarios, RolRepository roles) {
        this.usuarios = usuarios;
        this.roles = roles;
    }

    @Transactional
    public Usuario obtenerOCrear(OAuth2User perfil) {
        String correo = valor(perfil, "email");
        if (correo == null || correo.isBlank()) {
            throw new CuentaGoogleException("Tu cuenta de Google no tiene un correo asociado.");
        }
        correo = correo.trim().toLowerCase();

        String googleId = valor(perfil, "sub");
        String nombre   = primeraPalabra(valor(perfil, "given_name"), "Usuario");
        String apellido = primeraPalabra(valor(perfil, "family_name"), "Google");

        var existente = usuarios.findByCorreo(correo);

        if (existente.isPresent()) {
            Usuario u = existente.get();
            // Situacion 2: ese correo ya tiene una cuenta creada en la aplicacion.
            // No se fusionan de forma automatica: seria permitir tomar el control
            // de una cuenta ajena con solo tener el mismo correo en Google.
            if (u.esLocal()) {
                throw new CuentaGoogleException(
                        "Ese correo ya tiene una cuenta en FinMind. Inicia sesion con tu contrasena.");
            }
            u.registrarAcceso();
            return u;
        }

        Rol rolUsuario = roles.findByNombre(Rol.USUARIO)
                .orElseThrow(() -> new IllegalStateException(
                        "El rol " + Rol.USUARIO + " no existe. Verifica que la migracion V1 se aplico."));

        Usuario nuevo = Usuario.deGoogle(nombre, apellido, correo, rolUsuario, googleId);
        nuevo.registrarAcceso();
        return usuarios.save(nuevo);
    }

    private static String valor(OAuth2User perfil, String clave) {
        Object v = perfil.getAttributes().get(clave);
        return v == null ? null : v.toString();
    }

    private static String primeraPalabra(String texto, String porDefecto) {
        if (texto == null || texto.isBlank()) return porDefecto;
        String limpio = texto.trim();
        return limpio.length() > 80 ? limpio.substring(0, 80) : limpio;
    }

    public static class CuentaGoogleException extends RuntimeException {
        public CuentaGoogleException(String mensaje) { super(mensaje); }
    }
}
