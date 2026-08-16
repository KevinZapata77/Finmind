package com.finmind.common.security;

import com.finmind.usuarios.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptador entre la entidad Usuario y Spring Security.
 *
 * Guarda el id ademas del correo: asi los servicios obtienen el identificador
 * del usuario autenticado desde el token y nunca desde la URL o el cuerpo de la
 * peticion, que es lo que evita que alguien lea los datos de otro (SEG-04).
 */
public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final String correo;
    private final String contrasenaHash;
    private final boolean activo;
    private final boolean correoVerificado;
    private final String rol;

    public UsuarioPrincipal(Usuario usuario) {
        this.id = usuario.getId();
        this.correo = usuario.getCorreo();
        this.contrasenaHash = usuario.getContrasenaHash();
        this.activo = Boolean.TRUE.equals(usuario.getActivo());
        this.correoVerificado = usuario.estaVerificado();
        this.rol = usuario.getRol().getNombre();
    }

    public Long getId() {
        return id;
    }

    public String getRol() {
        return rol;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(rol));
    }

    @Override
    public String getPassword() {
        return contrasenaHash;
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * RN-011: una cuenta sin correo verificado se trata como bloqueada.
     * Spring Security responde con LockedException, que el manejador global
     * traduce a un mensaje que indica al usuario que debe verificar.
     */
    @Override
    public boolean isAccountNonLocked() {
        return correoVerificado;
    }

    public boolean tieneCorreoVerificado() {
        return correoVerificado;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
