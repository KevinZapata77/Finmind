package com.finmind.cuentas.entity;

import com.finmind.usuarios.entity.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Cuenta financiera de un usuario (RF-006 a RF-008).
 *
 * El saldo inicial se guarda; el saldo actual NO. Guardar un saldo actual
 * obligaria a actualizarlo en cada movimiento y cualquier fallo a mitad de
 * camino dejaria un numero que no corresponde a los movimientos registrados.
 * Se calcula al consultarlo, asi siempre cuadra con el historial.
 */
@Entity
@Table(name = "cuentas")
public class Cuenta {

    /** RN-020: este tipo representa deuda, no dinero disponible. */
    public static final String TARJETA_CREDITO = "TARJETA_CREDITO";

    public static final Set<String> TIPOS = Set.of(
            "EFECTIVO", "AHORROS", "CORRIENTE", "TARJETA_CREDITO", "BILLETERA_DIGITAL", "OTRO");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: al listar cuentas no se necesita traer el usuario completo.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String tipo;

    // NUMERIC(15,2) en la base. BigDecimal, nunca double: con double,
    // 0.1 + 0.2 no da 0.3 y en dinero eso es inaceptable (RN-010).
    @Column(name = "saldo_inicial", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoInicial;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(nullable = false)
    private Boolean activa;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    protected Cuenta() {
        // Requerido por JPA.
    }

    public Cuenta(Usuario usuario, String nombre, String tipo,
                  BigDecimal saldoInicial, String moneda) {
        this.usuario = usuario;
        this.nombre = nombre;
        this.tipo = tipo;
        this.saldoInicial = saldoInicial;
        this.moneda = moneda;
        this.activa = Boolean.TRUE;
        this.fechaCreacion = LocalDateTime.now();
    }

    /** RN-005: comprueba la propiedad antes de dejar tocar la cuenta. */
    public boolean perteneceA(Long usuarioId) {
        return usuario != null && usuario.getId().equals(usuarioId);
    }

    public boolean estaActiva() {
        return Boolean.TRUE.equals(activa);
    }

    public void editar(String nombre, String tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public void desactivar() {
        this.activa = Boolean.FALSE;
    }

    public void activar() {
        this.activa = Boolean.TRUE;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public String getMoneda() { return moneda; }
    public Boolean getActiva() { return activa; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
