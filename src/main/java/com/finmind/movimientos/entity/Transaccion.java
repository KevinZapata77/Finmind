package com.finmind.movimientos.entity;

import com.finmind.categorias.entity.Categoria;
import com.finmind.cuentas.entity.Cuenta;
import com.finmind.usuarios.entity.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un ingreso o un gasto (RF-012 a RF-016).
 *
 * Se guarda usuario_id ademas de cuenta_id aunque la cuenta ya sepa de quien es:
 * asi las consultas por usuario no necesitan pasar por cuentas, que es el filtro
 * mas frecuente de toda la aplicacion.
 */
@Entity
@Table(name = "transacciones")
public class Transaccion {

    public static final String INGRESO = "INGRESO";
    public static final String GASTO = "GASTO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    protected Transaccion() {
        // Requerido por JPA.
    }

    public Transaccion(Usuario usuario, Cuenta cuenta, Categoria categoria,
                       BigDecimal monto, LocalDate fecha, String descripcion) {
        this.usuario = usuario;
        this.cuenta = cuenta;
        this.categoria = categoria;
        // RN-002: el tipo lo manda la categoria, no el cliente. Si viniera aparte
        // se podria registrar un "ingreso" con categoria Alimentacion y los
        // reportes por categoria dejarian de cuadrar con el balance.
        this.tipo = categoria.getTipo();
        this.monto = monto;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.fechaRegistro = LocalDateTime.now();
    }

    public boolean esIngreso() {
        return INGRESO.equals(tipo);
    }

    /** Lo que este movimiento aporta al saldo: suma si entra, resta si sale. */
    public BigDecimal efectoEnElSaldo() {
        return esIngreso() ? monto : monto.negate();
    }

    public void editar(Cuenta cuenta, Categoria categoria, BigDecimal monto,
                       LocalDate fecha, String descripcion) {
        this.cuenta = cuenta;
        this.categoria = categoria;
        this.tipo = categoria.getTipo();
        this.monto = monto;
        this.fecha = fecha;
        this.descripcion = descripcion;
    }

    public Long getId() { return id; }
    public Cuenta getCuenta() { return cuenta; }
    public Categoria getCategoria() { return categoria; }
    public String getTipo() { return tipo; }
    public BigDecimal getMonto() { return monto; }
    public LocalDate getFecha() { return fecha; }
    public String getDescripcion() { return descripcion; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
}
