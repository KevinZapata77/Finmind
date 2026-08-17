package com.finmind.metas.entity;

import com.finmind.usuarios.entity.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Meta de ahorro (RF-032 a RF-034).
 *
 * A diferencia del saldo de una cuenta o del consumo de un presupuesto, aqui el
 * monto acumulado SI se guarda. Un abono a una meta no se deriva de ningun
 * movimiento: es una decision del usuario de apartar plata, sin contrapartida
 * en la tabla de transacciones. Si no se guardara, no habria de donde sacarlo.
 */
@Entity
@Table(name = "metas_ahorro")
public class MetaAhorro {

    public static final String EN_CURSO = "EN_CURSO";
    public static final String COMPLETADA = "COMPLETADA";
    public static final String CANCELADA = "CANCELADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "monto_objetivo", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoObjetivo;

    @Column(name = "monto_actual", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoActual;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(nullable = false, length = 12)
    private String estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    protected MetaAhorro() {
        // Requerido por JPA.
    }

    public MetaAhorro(Usuario usuario, String nombre, BigDecimal montoObjetivo, LocalDate fechaLimite) {
        this.usuario = usuario;
        this.nombre = nombre;
        this.montoObjetivo = montoObjetivo;
        this.montoActual = BigDecimal.ZERO;
        this.fechaLimite = fechaLimite;
        this.estado = EN_CURSO;
        this.fechaCreacion = LocalDateTime.now();
    }

    public boolean estaEnCurso() {
        return EN_CURSO.equals(estado);
    }

    /**
     * RN-017: solo una meta EN_CURSO admite abonos.
     * Al alcanzar el objetivo pasa sola a COMPLETADA.
     */
    public void abonar(BigDecimal monto) {
        this.montoActual = this.montoActual.add(monto);
        if (this.montoActual.compareTo(this.montoObjetivo) >= 0) {
            this.estado = COMPLETADA;
        }
    }

    /** Lo maximo que tiene sentido abonar hoy. */
    public BigDecimal loQueFalta() {
        return montoObjetivo.subtract(montoActual).max(BigDecimal.ZERO);
    }

    public void cancelar() {
        this.estado = CANCELADA;
    }

    public void editar(String nombre, BigDecimal montoObjetivo, LocalDate fechaLimite) {
        this.nombre = nombre;
        this.montoObjetivo = montoObjetivo;
        this.fechaLimite = fechaLimite;
        // Subir el objetivo por encima de lo ya ahorrado reabre la meta.
        if (this.montoActual.compareTo(this.montoObjetivo) < 0 && COMPLETADA.equals(this.estado)) {
            this.estado = EN_CURSO;
        }
    }

    public BigDecimal porcentajeAvance() {
        return montoActual.multiply(new BigDecimal("100"))
                .divide(montoObjetivo, 1, RoundingMode.HALF_UP)
                .min(new BigDecimal("100.0"));
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public BigDecimal getMontoObjetivo() { return montoObjetivo; }
    public BigDecimal getMontoActual() { return montoActual; }
    public LocalDate getFechaLimite() { return fechaLimite; }
    public String getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
