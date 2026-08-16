package com.finmind.identidad.service;

import com.finmind.identidad.entity.CodigoVerificacion;
import com.finmind.identidad.repository.CodigoVerificacionRepository;
import com.finmind.usuarios.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Emision y validacion de codigos de un solo uso.
 *
 * Reglas que implementa:
 *  RN-012  codigo numerico de seis digitos, un solo uso, 15 minutos de vigencia
 *  DT-06   maximo cinco intentos por codigo
 *  AME-07  emitir uno nuevo invalida el anterior, lo que corta el abuso del reenvio
 */
@Service
public class ServicioCodigos {

    // SecureRandom y no Random: un generador predecible haria adivinable el codigo.
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final CodigoVerificacionRepository repositorio;
    private final int vigenciaMinutos;
    private final int intentosMaximos;

    public ServicioCodigos(CodigoVerificacionRepository repositorio,
                           @Value("${finmind.codigo.vigencia-minutos:15}") int vigenciaMinutos,
                           @Value("${finmind.codigo.intentos-maximos:5}") int intentosMaximos) {
        this.repositorio = repositorio;
        this.vigenciaMinutos = vigenciaMinutos;
        this.intentosMaximos = intentosMaximos;
    }

    public int getVigenciaMinutos() {
        return vigenciaMinutos;
    }

    /** Invalida el codigo anterior del mismo tipo y emite uno nuevo. */
    @Transactional
    public String emitir(Usuario usuario, String tipo) {
        repositorio.invalidarAnteriores(usuario.getId(), tipo, LocalDateTime.now());
        String codigo = String.format("%06d", ALEATORIO.nextInt(1_000_000));
        repositorio.save(new CodigoVerificacion(usuario, codigo, tipo, vigenciaMinutos));
        return codigo;
    }

    /**
     * Valida el codigo y lo consume si es correcto.
     *
     * Devuelve el resultado en lugar de lanzar excepcion para que quien llama
     * decida el mensaje: los flujos de verificacion y recuperacion responden
     * distinto ante el mismo fallo.
     */
    @Transactional
    public Resultado validarYConsumir(Long usuarioId, String tipo, String codigoRecibido) {
        Optional<CodigoVerificacion> encontrado =
                repositorio.findByUsuarioIdAndTipoAndUsadoEnIsNull(usuarioId, tipo);

        if (encontrado.isEmpty()) return Resultado.NO_EXISTE;

        CodigoVerificacion codigo = encontrado.get();

        if (codigo.superoIntentos(intentosMaximos)) return Resultado.BLOQUEADO;
        if (!codigo.estaVigente())                  return Resultado.VENCIDO;

        if (!codigo.coincide(codigoRecibido)) {
            codigo.registrarIntentoFallido();
            return Resultado.NO_COINCIDE;
        }

        codigo.consumir();
        return Resultado.VALIDO;
    }

    public enum Resultado {
        VALIDO,
        NO_EXISTE,
        VENCIDO,
        NO_COINCIDE,
        BLOQUEADO
    }
}
