package com.finmind.administracion.entity;

import com.finmind.usuarios.entity.Usuario;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Registro de lo que hace un administrador (RF-024).
 *
 * Sin esta tabla, un administrador podria desactivar la cuenta de alguien y no
 * quedaria rastro de quien fue ni cuando. El poder de administrar solo es
 * aceptable si queda registrado.
 *
 * No tiene borrado ni edicion a proposito: un registro de auditoria que se
 * puede modificar no sirve como auditoria.
 */
@Entity
@Table(name = "auditoria_admin")
public class AuditoriaAdmin {

    public static final String ACTIVAR_USUARIO = "ACTIVAR_USUARIO";
    public static final String DESACTIVAR_USUARIO = "DESACTIVAR_USUARIO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private Usuario admin;

    @Column(nullable = false, length = 60)
    private String accion;

    @Column(nullable = false, length = 60)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(length = 255)
    private String detalle;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected AuditoriaAdmin() {
        // Requerido por JPA.
    }

    public AuditoriaAdmin(Usuario admin, String accion, String entidad, Long entidadId, String detalle) {
        this.admin = admin;
        this.accion = accion;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.detalle = detalle;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Usuario getAdmin() { return admin; }
    public String getAccion() { return accion; }
    public String getEntidad() { return entidad; }
    public Long getEntidadId() { return entidadId; }
    public String getDetalle() { return detalle; }
    public LocalDateTime getFecha() { return fecha; }
}
