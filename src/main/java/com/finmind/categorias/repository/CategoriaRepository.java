package com.finmind.categorias.repository;

import com.finmind.categorias.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /**
     * Las que el usuario puede usar: las suyas mas las del sistema.
     * Es la consulta que alimenta el desplegable al registrar un movimiento.
     */
    @Query("""
           SELECT c FROM Categoria c
           WHERE (c.usuario.id = :usuarioId OR c.usuario IS NULL)
             AND (:soloActivas = false OR c.activa = true)
           ORDER BY c.tipo, c.nombre
           """)
    List<Categoria> disponiblesPara(@Param("usuarioId") Long usuarioId,
                                    @Param("soloActivas") boolean soloActivas);

    @Query("""
           SELECT c FROM Categoria c
           WHERE c.id = :id AND (c.usuario.id = :usuarioId OR c.usuario IS NULL)
           """)
    Optional<Categoria> usablePor(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    /** Solo las propias: es lo unico que se puede editar o desactivar. */
    Optional<Categoria> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndNombreIgnoreCaseAndTipo(Long usuarioId, String nombre, String tipo);

    boolean existsByUsuarioIdAndNombreIgnoreCaseAndTipoAndIdNot(
            Long usuarioId, String nombre, String tipo, Long id);

    /** Choque con una del sistema: tampoco se admite, para no tener dos "Salario". */
    boolean existsByUsuarioIsNullAndNombreIgnoreCaseAndTipo(String nombre, String tipo);

    /**
     * Borrado en bloque de las categorias de usuarios, respetando las del sistema.
     *
     * Tiene que ser una sentencia en bloque como esta y no deleteAll(entidades):
     * el borrado por entidades se aplaza hasta el commit, y para entonces el
     * borrado en bloque de usuarios ya se ejecuto y la llave foranea revento.
     * Solo lo usa la limpieza entre pruebas.
     */
    @Modifying
    @Query("DELETE FROM Categoria c WHERE c.usuario IS NOT NULL")
    void borrarLasDeUsuarios();
}
