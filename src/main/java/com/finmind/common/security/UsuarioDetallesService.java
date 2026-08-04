package com.finmind.common.security;

import com.finmind.usuarios.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioDetallesService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetallesService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * El mensaje de error es deliberadamente generico: decir "el correo no existe"
     * permite averiguar que cuentas estan registradas.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        return usuarioRepository.findByCorreo(correo)
                .map(UsuarioPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales invalidas"));
    }
}
