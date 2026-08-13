package com.finmind.usuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Rol de la aplicacion. Se corresponde con la tabla "roles" creada por
 * la migracion V1. El id es Short porque en PostgreSQL la columna es SMALLINT;
 * PostgreSQL no tiene TINYINT. Los dos roles existentes son ROLE_USUARIO y ROLE_ADMIN.
 *
 * El administrador NO tiene acceso a datos financieros de los usuarios (RF-26);
 * esa restriccion se aplica en la capa de servicio y en SecurityConfig, no aqui.
 */
@Entity
@Table(name = "roles")
public class Rol {

    public static final String USUARIO = "ROLE_USUARIO";
    public static final String ADMIN = "ROLE_ADMIN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Short id;

    @Column(name = "nombre", nullable = false, length = 30, unique = true)
    private String nombre;

    @Column(name = "descripcion", length = 150)
    private String descripcion;

    protected Rol() {
        // Requerido por JPA.
    }

    public Rol(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public Short getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
