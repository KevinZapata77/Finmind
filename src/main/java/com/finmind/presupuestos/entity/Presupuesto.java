package com.finmind.presupuestos.entity;

import com.finmind.categorias.entity.Categoria;
import com.finmind.usuarios.entity.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/**
 * Limite de gasto por categoria y mes (RF-017 a RF-020).
 *
 * El consumo NO se guarda aqui: se calcula sumando los movimientos del periodo
 * (RN-009). Guardarlo obligaria a actualizar esta fila en cada movimiento y
 * cualquier fallo a mitad de camino dejaria un numero que no corresponde a los
 * gastos registrados. Es la misma decision que en el saldo de las cuentas.
 */
@Entity
@Table(name = "presupuestos")
public class Presupuesto {

    public static final Set<String> PERIODOS = Set.of("MENSUAL", "QUINCENAL", "SEMANAL");

    /** Umbral de aviso de RF-019: a partir de aqui se advierte antes de pasarse. */
    public static final BigDecimal UMBRAL_ALERTA = new BigDecimal("80");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "monto_limite", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoLimite;

    @Column(nullable = false, length = 10)
    private String periodo;

    @Column(nullable = false)
    private Short anio;

    @Column(nullable = false)
    private Short mes;

    @Column(nullable = false)
    private Boolean activo;

    protected Presupuesto() {
        // Requerido por JPA.
    }

    public Presupuesto(Usuario usuario, Categoria categoria, BigDecimal montoLimite,
                       String periodo, Short anio, Short mes) {
        this.usuario = usuario;
        this.categoria = categoria;
        this.montoLimite = montoLimite;
        this.periodo = periodo;
        this.anio = anio;
        this.mes = mes;
        this.activo = Boolean.TRUE;
    }

    public boolean estaActivo() {
        return Boolean.TRUE.equals(activo);
    }

    /** El periodo y la categoria no se editan: serian otro presupuesto distinto. */
    public void editar(BigDecimal montoLimite) {
        this.montoLimite = montoLimite;
    }

    public void desactivar() { this.activo = Boolean.FALSE; }
    public void activar() { this.activo = Boolean.TRUE; }

    /** Porcentaje consumido (RF-018). Puede pasar de 100 si el usuario se excedio. */
    public BigDecimal porcentajeConsumido(BigDecimal consumo) {
        return consumo.multiply(new BigDecimal("100"))
                .divide(montoLimite, 1, RoundingMode.HALF_UP);
    }

    /** Lo que queda. Negativo significa que ya se paso del limite. */
    public BigDecimal disponible(BigDecimal consumo) {
        return montoLimite.subtract(consumo);
    }

    public Long getId() { return id; }
    public Categoria getCategoria() { return categoria; }
    public BigDecimal getMontoLimite() { return montoLimite; }
    public String getPeriodo() { return periodo; }
    public Short getAnio() { return anio; }
    public Short getMes() { return mes; }
    public Boolean getActivo() { return activo; }
}
