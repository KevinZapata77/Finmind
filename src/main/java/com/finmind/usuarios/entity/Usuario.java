package com.finmind.usuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Usuario de FinMind. Se corresponde con la tabla "usuarios" de la migracion V1.
 *
 * La contrasena se guarda siempre como hash BCrypt (SEG-01). El campo se llama
 * contrasenaHash y no "contrasena" justamente para que nadie asuma que ahi cabe
 * texto plano.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 80)
    private String apellido;

    @Column(name = "correo", nullable = false, length = 120, unique = true)
    private String correo;

    // Admite nulo: quien entra con Google no define contrasena en FinMind
    @Column(name = "contrasena_hash", length = 100)
    private String contrasenaHash;

    @Column(name = "correo_verificado", nullable = false)
    private Boolean correoVerificado = Boolean.FALSE;

    @Column(name = "proveedor", nullable = false, length = 10)
    private String proveedor = LOCAL;

    @Column(name = "proveedor_id", length = 120)
    private String proveedorId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(name = "activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    public static final String LOCAL = "LOCAL";
    public static final String GOOGLE = "GOOGLE";

    protected Usuario() {
        // Requerido por JPA.
    }

    public Usuario(String nombre, String apellido, String correo, String contrasenaHash, Rol rol) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
        this.activo = Boolean.TRUE;
    }

    @PrePersist
    void alCrear() {
        LocalDateTime ahora = LocalDateTime.now();
        this.fechaCreacion = ahora;
        this.fechaActualizacion = ahora;
        if (this.activo == null) {
            this.activo = Boolean.TRUE;
        }
    }

    @PreUpdate
    void alActualizar() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    /** RN-011: solo un correo verificado habilita el inicio de sesion. */
    public boolean estaVerificado() {
        return Boolean.TRUE.equals(correoVerificado);
    }

    public void marcarCorreoVerificado() {
        this.correoVerificado = Boolean.TRUE;
    }

    public Boolean getCorreoVerificado() { return correoVerificado; }

    /**
     * Usuario creado a partir de una cuenta de Google.
     * Nace verificado: Google ya comprobo que esa direccion existe y le
     * pertenece, asi que pedirle un codigo seria pedir dos veces lo mismo.
     */
    public static Usuario deGoogle(String nombre, String apellido, String correo,
                                   Rol rol, String proveedorId) {
        Usuario u = new Usuario();
        u.nombre = nombre;
        u.apellido = apellido;
        u.correo = correo;
        u.rol = rol;
        u.proveedor = GOOGLE;
        u.proveedorId = proveedorId;
        u.correoVerificado = Boolean.TRUE;
        u.contrasenaHash = null;
        u.activo = Boolean.TRUE;
        return u;
    }

    public boolean esLocal() {
        return LOCAL.equals(proveedor);
    }

    public String getProveedor() { return proveedor; }
    public String getProveedorId() { return proveedorId; }

    public void registrarAcceso() {
        this.ultimoAcceso = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getCorreo() {
        return correo;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public Rol getRol() {
        return rol;
    }

    public Boolean getActivo() {
        return activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
