package com.finmind.categorias.entity;

import com.finmind.usuarios.entity.Usuario;
import jakarta.persistence.*;

import java.util.Set;

/**
 * Categoria de un movimiento (RF-009 a RF-011).
 *
 * usuario_id NULO significa categoria del sistema: la ven todos, nadie la edita.
 * Es la razon de que el campo sea nullable en vez de tener un usuario "sistema":
 * un usuario ficticio obligaria a excluirlo a mano en cada consulta de usuarios.
 */
@Entity
@Table(name = "categorias")
public class Categoria {

    public static final String INGRESO = "INGRESO";
    public static final String GASTO = "GASTO";
    public static final Set<String> TIPOS = Set.of(INGRESO, GASTO);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nulo en las categorias del sistema. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(length = 40)
    private String icono;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(nullable = false)
    private Boolean activa;

    protected Categoria() {
        // Requerido por JPA.
    }

    public Categoria(Usuario usuario, String nombre, String tipo, String icono, String colorHex) {
        this.usuario = usuario;
        this.nombre = nombre;
        this.tipo = tipo;
        this.icono = icono;
        this.colorHex = colorHex;
        this.activa = Boolean.TRUE;
    }

    /** Las del sistema no tienen dueno y por eso nadie puede modificarlas. */
    public boolean esDelSistema() {
        return usuario == null;
    }

    /** RN-005 ampliada: una categoria del sistema es "de todos" solo para leer. */
    public boolean puedeUsarla(Long usuarioId) {
        return esDelSistema() || usuario.getId().equals(usuarioId);
    }

    public boolean puedeEditarla(Long usuarioId) {
        return !esDelSistema() && usuario.getId().equals(usuarioId);
    }

    public boolean estaActiva() {
        return Boolean.TRUE.equals(activa);
    }

    public void editar(String nombre, String icono, String colorHex) {
        this.nombre = nombre;
        this.icono = icono;
        this.colorHex = colorHex;
    }

    public void desactivar() { this.activa = Boolean.FALSE; }
    public void activar() { this.activa = Boolean.TRUE; }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public String getIcono() { return icono; }
    public String getColorHex() { return colorHex; }
    public Boolean getActiva() { return activa; }
}
