package com.finmind.metas.service;

import com.finmind.common.exception.RecursoNoEncontradoException;
import com.finmind.metas.dto.*;
import com.finmind.metas.entity.MetaAhorro;
import com.finmind.metas.repository.MetaAhorroRepository;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Metas de ahorro (RF-032 a RF-034). */
@Service
public class ServicioMetas {

    private final MetaAhorroRepository metas;
    private final UsuarioRepository usuarios;

    public ServicioMetas(MetaAhorroRepository metas, UsuarioRepository usuarios) {
        this.metas = metas;
        this.usuarios = usuarios;
    }

    @Transactional
    public MetaResponse crear(Long usuarioId, MetaRequest p) {
        String nombre = p.nombre().trim();
        if (metas.existsByUsuarioIdAndNombreIgnoreCase(usuarioId, nombre)) {
            throw new MetaRepetidaException("Ya tienes una meta con ese nombre");
        }
        Usuario dueno = usuarios.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException("El token es valido pero el usuario ya no existe"));
        return MetaResponse.de(metas.save(
                new MetaAhorro(dueno, nombre, p.montoObjetivo(), p.fechaLimite())));
    }

    @Transactional(readOnly = true)
    public List<MetaResponse> listar(Long usuarioId, String estado) {
        List<MetaAhorro> propias = (estado == null || estado.isBlank())
                ? metas.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId)
                : metas.findByUsuarioIdAndEstadoOrderByFechaCreacionDesc(
                        usuarioId, estado.trim().toUpperCase());
        return propias.stream().map(MetaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public MetaResponse consultar(Long usuarioId, Long id) {
        return MetaResponse.de(exigirPropia(usuarioId, id));
    }

    /**
     * RF-033 y RN-017. Solo se abona a una meta EN_CURSO.
     *
     * Un abono por encima de lo que falta se rechaza en vez de recortarse en
     * silencio: si el usuario escribio 500.000 donde faltaban 50.000, lo mas
     * probable es que se equivoco de meta o de cifra, y aceptarlo a medias le
     * ocultaria el error.
     */
    @Transactional
    public MetaResponse abonar(Long usuarioId, Long id, AbonoRequest p) {
        MetaAhorro meta = exigirPropia(usuarioId, id);

        if (!meta.estaEnCurso()) {
            throw new MetaCerradaException(
                    "Esta meta ya esta " + meta.getEstado().toLowerCase() + " y no admite abonos");
        }
        if (p.monto().compareTo(meta.loQueFalta()) > 0) {
            throw new AbonoExcesivoException(
                    "El abono supera lo que falta. Para completarla basta con " + meta.loQueFalta());
        }
        meta.abonar(p.monto());
        return MetaResponse.de(meta);
    }

    @Transactional
    public MetaResponse editar(Long usuarioId, Long id, MetaRequest p) {
        MetaAhorro meta = exigirPropia(usuarioId, id);
        String nombre = p.nombre().trim();
        if (metas.existsByUsuarioIdAndNombreIgnoreCaseAndIdNot(usuarioId, nombre, id)) {
            throw new MetaRepetidaException("Ya tienes otra meta con ese nombre");
        }
        if (p.montoObjetivo().compareTo(meta.getMontoActual()) < 0) {
            throw new ObjetivoMenorQueLoAhorradoException(
                    "El objetivo no puede quedar por debajo de lo que ya ahorraste ("
                            + meta.getMontoActual() + ")");
        }
        meta.editar(nombre, p.montoObjetivo(), p.fechaLimite());
        return MetaResponse.de(meta);
    }

    /** RF-034. No se borra: lo ahorrado sigue siendo parte del historial. */
    @Transactional
    public MetaResponse cancelar(Long usuarioId, Long id) {
        MetaAhorro meta = exigirPropia(usuarioId, id);
        meta.cancelar();
        return MetaResponse.de(meta);
    }

    private MetaAhorro exigirPropia(Long usuarioId, Long id) {
        return metas.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La meta no existe"));
    }

    /** 409 */
    public static class MetaRepetidaException extends RuntimeException {
        public MetaRepetidaException(String m) { super(m); }
    }
    /** 409 */
    public static class MetaCerradaException extends RuntimeException {
        public MetaCerradaException(String m) { super(m); }
    }
    /** 400 */
    public static class AbonoExcesivoException extends RuntimeException {
        public AbonoExcesivoException(String m) { super(m); }
    }
    /** 400 */
    public static class ObjetivoMenorQueLoAhorradoException extends RuntimeException {
        public ObjetivoMenorQueLoAhorradoException(String m) { super(m); }
    }
}
