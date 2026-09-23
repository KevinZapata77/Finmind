package com.finmind.usuarios.repository;

import com.finmind.usuarios.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    // --- Administracion (RF-023, RF-024). Solo conteos y listados, nunca datos financieros.
    java.util.List<Usuario> findAllByOrderByFechaCreacionDesc();

    long countByActivoTrueAndCorreoVerificadoTrue();

    long countByCorreoVerificadoFalse();

    long countByActivoFalse();

    /*
      Conteos de actividad para el resumen de administracion (RF-056).

      Son numeros agregados: cuentan filas, nunca devuelven datos de nadie. Un
      administrador necesita saber si la plataforma se esta usando, no quien la
      usa — y la diferencia entre esas dos cosas es la que separa una pantalla
      de operacion de una de vigilancia.

      La fecha llega como parametro en vez de calcularse aqui con NOW() para
      que las pruebas puedan fijar el momento y no dependan del reloj.
    */

    /** Cuentas creadas desde una fecha. Sirve para "registros de los ultimos 7 dias". */
    long countByFechaCreacionAfter(LocalDateTime desde);

    /** Cuentas que entraron desde una fecha. Es la medida real de uso. */
    long countByUltimoAccesoAfter(LocalDateTime desde);

    /**
     * Cuentas que nunca iniciaron sesion.
     *
     * Es el numero mas revelador de los cuatro: alguien que se registro y no
     * volvio significa que algo se rompio entre el registro y el primer uso
     * —el correo que no llega, por ejemplo— y ese hueco no se ve en ningun
     * otro contador.
     */
    long countByUltimoAccesoIsNull();
}
