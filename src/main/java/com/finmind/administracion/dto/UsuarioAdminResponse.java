package com.finmind.administracion.dto;

import com.finmind.usuarios.entity.Usuario;

import java.time.LocalDateTime;

/**
 * Vista que el administrador tiene de un usuario.
 *
 * RN-005 sigue en pie: NO incluye saldos, movimientos, deudas ni nada
 * financiero. El administrador gestiona el acceso a la plataforma, no el
 * dinero de las personas. Tampoco expone el hash de la contrasena.
 */
public record UsuarioAdminResponse(
        Long id, String nombre, String apellido, String correo, String rol,
        Boolean activo, Boolean correoVerificado, String proveedor,
        LocalDateTime fechaCreacion, LocalDateTime ultimoAcceso, String estado
) {
    public static UsuarioAdminResponse de(Usuario u) {
        String estado = !Boolean.TRUE.equals(u.getActivo()) ? "Desactivada"
                : !u.estaVerificado() ? "Sin verificar"
                : "Activa";
        return new UsuarioAdminResponse(u.getId(), u.getNombre(), u.getApellido(), u.getCorreo(),
                u.getRol().getNombre(), u.getActivo(), u.getCorreoVerificado(), u.getProveedor(),
                u.getFechaCreacion(), u.getUltimoAcceso(), estado);
    }
}
