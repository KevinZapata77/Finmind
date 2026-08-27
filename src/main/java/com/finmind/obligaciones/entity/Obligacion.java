package com.finmind.obligaciones.entity;

import com.finmind.usuarios.entity.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Deuda del usuario (RF-035 a RF-039).
 *
 * Una cuenta responde "cuanto tengo". Una obligacion responde "cuanto debo, a
 * quien, a que tasa y cuando vence la proxima cuota". Por eso son tablas
 * distintas y no un tipo mas de cuenta.
 */
@Entity
@Table(name = "obligaciones")
public class Obligacion {

    /**
     * Creditos y prestamos con cuota pactada.
     *
     * TARJETA_CREDITO salio de esta lista en la V7 (DEF-15). Estaba aqui y
     * tambien en los tipos de cuenta, asi que quien registrara la misma tarjeta
     * en los dos modulos veia su deuda restada dos veces del patrimonio.
     *
     * Ahora la tarjeta vive solo como cuenta, donde tiene cupo, la deuda sube al
     * comprar y baja al pagar, y los gastos quedan categorizados. Ademas una
     * tarjeta no tiene cuota fija ni un numero de cuotas: se paga lo que se
     * debe, todo o una parte. Meterla aqui obligaba a inventar una cuota que no
     * existe.
     */
    public static final Set<String> TIPOS = Set.of(
            "PRESTAMO_BANCARIO", "PRESTAMO_PERSONAL",
            "CREDITO_HIPOTECARIO", "CREDITO_VEHICULO", "OTRO");

    public static final String ACTIVA = "ACTIVA";
    public static final String PAGADA = "PAGADA";
    public static final String CANCELADA = "CANCELADA";

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal MESES = new BigDecimal("12");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String acreedor;

    @Column(nullable = false, length = 22)
    private String tipo;

    @Column(name = "monto_original", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoOriginal;

    @Column(name = "saldo_pendiente", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoPendiente;

    /** Porcentaje efectivo anual. 24.5000 es 24.5%. */
    @Column(name = "tasa_anual", nullable = false, precision = 7, scale = 4)
    private BigDecimal tasaAnual;

    @Column(name = "cuota_mensual", nullable = false, precision = 15, scale = 2)
    private BigDecimal cuotaMensual;

    @Column(name = "dia_pago", nullable = false)
    private Short diaPago;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false, length = 10)
    private String estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    protected Obligacion() {
        // Requerido por JPA.
    }

    public Obligacion(Usuario usuario, String nombre, String acreedor, String tipo,
                      BigDecimal montoOriginal, BigDecimal tasaAnual,
                      BigDecimal cuotaMensual, Short diaPago, LocalDate fechaInicio) {
        this.usuario = usuario;
        this.nombre = nombre;
        this.acreedor = acreedor;
        this.tipo = tipo;
        this.montoOriginal = montoOriginal;
        this.saldoPendiente = montoOriginal;   // se nace debiendo todo
        this.tasaAnual = tasaAnual;
        this.cuotaMensual = cuotaMensual;
        this.diaPago = diaPago;
        this.fechaInicio = fechaInicio;
        this.estado = ACTIVA;
        this.fechaCreacion = LocalDateTime.now();
    }

    /** RN-005. */
    public boolean perteneceA(Long usuarioId) {
        return usuario != null && usuario.getId().equals(usuarioId);
    }

    public boolean estaActiva() {
        return ACTIVA.equals(estado);
    }

    /**
     * RN-018. Interes del periodo = saldo x (tasa anual / 12).
     * Se redondea HALF_UP a dos decimales, igual que un extracto bancario.
     */
    public BigDecimal interesDelPeriodo() {
        if (tasaAnual.signum() == 0) return BigDecimal.ZERO.setScale(2);
        return saldoPendiente
                .multiply(tasaAnual)
                .divide(CIEN, 10, RoundingMode.HALF_UP)
                .divide(MESES, 2, RoundingMode.HALF_UP);
    }

    /** Lo maximo que tiene sentido pagar hoy: todo el capital mas su interes. */
    public BigDecimal totalParaSaldar() {
        return saldoPendiente.add(interesDelPeriodo());
    }

    /** Aplica el abono a capital y cierra la obligacion si ya no queda nada. */
    public void descontar(BigDecimal abonoCapital) {
        this.saldoPendiente = this.saldoPendiente.subtract(abonoCapital);
        if (this.saldoPendiente.signum() == 0) {
            this.estado = PAGADA;
        }
    }

    public void cancelar() {
        this.estado = CANCELADA;
    }

    public void editar(String nombre, String acreedor, BigDecimal cuotaMensual, Short diaPago) {
        this.nombre = nombre;
        this.acreedor = acreedor;
        this.cuotaMensual = cuotaMensual;
        this.diaPago = diaPago;
    }

    /** Porcentaje ya pagado del capital. Lo calcula el servidor, no el cliente. */
    public BigDecimal porcentajePagado() {
        return montoOriginal.subtract(saldoPendiente)
                .multiply(CIEN)
                .divide(montoOriginal, 1, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getNombre() { return nombre; }
    public String getAcreedor() { return acreedor; }
    public String getTipo() { return tipo; }
    public BigDecimal getMontoOriginal() { return montoOriginal; }
    public BigDecimal getSaldoPendiente() { return saldoPendiente; }
    public BigDecimal getTasaAnual() { return tasaAnual; }
    public BigDecimal getCuotaMensual() { return cuotaMensual; }
    public Short getDiaPago() { return diaPago; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public String getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
