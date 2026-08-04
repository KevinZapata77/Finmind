package com.finmind.auth.dto;

import com.finmind.usuarios.entity.Usuario;

/**
 * Vista publica de un usuario. Nunca incluye el hash de la contrasena.
 */
public record UsuarioResponse(
        Long id,
        String nombre,
        String apellido,
        String correo,
        String rol
) {
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                usuario.getRol().getNombre());
    }
}
