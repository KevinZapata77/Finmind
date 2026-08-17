package com.finmind.categorias.service;

import com.finmind.categorias.dto.*;
import com.finmind.categorias.entity.Categoria;
import com.finmind.categorias.repository.CategoriaRepository;
import com.finmind.common.exception.RecursoNoEncontradoException;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Categorias propias y del sistema (RF-009 a RF-011). */
@Service
public class ServicioCategorias {

    private final CategoriaRepository categorias;
    private final UsuarioRepository usuarios;

    public ServicioCategorias(CategoriaRepository categorias, UsuarioRepository usuarios) {
        this.categorias = categorias;
        this.usuarios = usuarios;
    }

    @Transactional
    public CategoriaResponse crear(Long usuarioId, CategoriaRequest p) {
        String nombre = p.nombre().trim();
        String tipo = normalizarTipo(p.tipo());

        // Choca tanto con las propias como con las del sistema: si no, el usuario
        // podria crear un segundo "Salario" y no distinguirlos en el desplegable.
        if (categorias.existsByUsuarioIdAndNombreIgnoreCaseAndTipo(usuarioId, nombre, tipo)
                || categorias.existsByUsuarioIsNullAndNombreIgnoreCaseAndTipo(nombre, tipo)) {
            throw new CategoriaRepetidaException("Ya existe una categoria de " + tipo.toLowerCase()
                    + " con ese nombre");
        }
        Usuario dueno = usuarios.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException("El token es valido pero el usuario ya no existe"));

        return CategoriaResponse.de(categorias.save(
                new Categoria(dueno, nombre, tipo, p.icono(), p.colorHex())));
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(Long usuarioId, String tipo, boolean soloActivas) {
        return categorias.disponiblesPara(usuarioId, soloActivas).stream()
                .filter(c -> tipo == null || c.getTipo().equals(normalizarTipo(tipo)))
                .map(CategoriaResponse::de)
                .toList();
    }

    @Transactional
    public CategoriaResponse editar(Long usuarioId, Long id, EditarCategoriaRequest p) {
        Categoria c = buscarPropia(usuarioId, id);
        String nombre = p.nombre().trim();
        if (categorias.existsByUsuarioIdAndNombreIgnoreCaseAndTipoAndIdNot(usuarioId, nombre, c.getTipo(), id)) {
            throw new CategoriaRepetidaException("Ya tienes otra categoria con ese nombre");
        }
        c.editar(nombre, p.icono(), p.colorHex());
        return CategoriaResponse.de(c);
    }

    @Transactional
    public CategoriaResponse desactivar(Long usuarioId, Long id) {
        Categoria c = buscarPropia(usuarioId, id);
        c.desactivar();
        return CategoriaResponse.de(c);
    }

    @Transactional
    public CategoriaResponse activar(Long usuarioId, Long id) {
        Categoria c = buscarPropia(usuarioId, id);
        c.activar();
        return CategoriaResponse.de(c);
    }

    /** Para el modulo de movimientos: valida que el usuario pueda usar esa categoria. */
    @Transactional(readOnly = true)
    public Categoria exigirUsable(Long usuarioId, Long categoriaId) {
        Categoria c = categorias.usablePor(categoriaId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La categoria no existe"));
        if (!c.estaActiva()) {
            throw new CategoriaInactivaException(
                    "La categoria '" + c.getNombre() + "' esta desactivada. Actívala o elige otra.");
        }
        return c;
    }

    /**
     * Solo las propias. Una del sistema responde 404 y no 403: el usuario no
     * necesita saber que existe una categoria que no puede tocar, y ademas
     * "no existe para vos" es exactamente lo que ocurre.
     */
    private Categoria buscarPropia(Long usuarioId, Long id) {
        return categorias.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La categoria no existe o es del sistema y no se puede modificar"));
    }

    private String normalizarTipo(String tipo) {
        String limpio = tipo == null ? "" : tipo.trim().toUpperCase();
        if (!Categoria.TIPOS.contains(limpio)) {
            throw new TipoDeCategoriaInvalidoException("El tipo debe ser INGRESO o GASTO");
        }
        return limpio;
    }

    /** 409 */
    public static class CategoriaRepetidaException extends RuntimeException {
        public CategoriaRepetidaException(String m) { super(m); }
    }
    /** 400 */
    public static class TipoDeCategoriaInvalidoException extends RuntimeException {
        public TipoDeCategoriaInvalidoException(String m) { super(m); }
    }
    /** 409 */
    public static class CategoriaInactivaException extends RuntimeException {
        public CategoriaInactivaException(String m) { super(m); }
    }
}
