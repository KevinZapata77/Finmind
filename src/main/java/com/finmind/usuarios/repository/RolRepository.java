package com.finmind.usuarios.repository;

import com.finmind.usuarios.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Byte> {

    Optional<Rol> findByNombre(String nombre);
}
