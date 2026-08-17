package com.finmind.usuarios.repository;

import com.finmind.usuarios.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    // --- Administracion (RF-023, RF-024). Solo conteos y listados, nunca datos financieros.
    java.util.List<Usuario> findAllByOrderByFechaCreacionDesc();

    long countByActivoTrueAndCorreoVerificadoTrue();

    long countByCorreoVerificadoFalse();

    long countByActivoFalse();
}
