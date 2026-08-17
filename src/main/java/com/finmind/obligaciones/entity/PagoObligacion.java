package com.finmind.obligaciones.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un pago aplicado a una obligacion, ya descompuesto (RN-018).
 *
 * Se guarda el desglose y no solo el total: reconstruir despues cuanto fue
 * interes exigiria recalcular toda la historia con las tasas de cada fecha,
 * y el resultado dejaria de ser auditable.
 */
@Entity
@Table(name = "pagos_obligacion")
public class PagoObligacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obligacion_id", nullable = false)
    private Obligacion obligacion;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal interes;

    @Column(name = "abono_capital", nullable = false, precision = 15, scale = 2)
    private BigDecimal abonoCapital;

    @Column(name = "saldo_resultante", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoResultante;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    protected PagoObligacion() {
        // Requerido por JPA.
    }

    public PagoObligacion(Obligacion obligacion, BigDecimal monto, BigDecimal interes,
                          BigDecimal abonoCapital, BigDecimal saldoResultante,
                          LocalDate fecha, String descripcion) {
        this.obligacion = obligacion;
        this.monto = monto;
        this.interes = interes;
        this.abonoCapital = abonoCapital;
        this.saldoResultante = saldoResultante;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.fechaRegistro = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public BigDecimal getMonto() { return monto; }
    public BigDecimal getInteres() { return interes; }
    public BigDecimal getAbonoCapital() { return abonoCapital; }
    public BigDecimal getSaldoResultante() { return saldoResultante; }
    public LocalDate getFecha() { return fecha; }
    public String getDescripcion() { return descripcion; }
}
