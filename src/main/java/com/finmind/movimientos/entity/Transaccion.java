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

    /**
     * RN-022. Movimiento entre dos cuentas del mismo usuario.
     *
     * No es ingreso ni gasto: el dinero no entro ni salio del patrimonio, cambio
     * de sitio. Por eso queda fuera del balance del mes, de la composicion del
     * gasto y del consumo de presupuesto, que filtran por INGRESO o GASTO.
     *
     * Se creo para poder abonar a una tarjeta de credito (DEF-16). Antes habia
     * que registrarlo como ingreso sobre la tarjeta, lo que inflaba los ingresos
     * del mes y no descontaba el dinero de la cuenta de donde salio.
     */
    public static final String TRANSFERENCIA = "TRANSFERENCIA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    /**
     * Nula solo en las transferencias: mover dinero de una cuenta a otra no
     * pertenece a ninguna categoria de gasto. La base lo garantiza con la
     * restriccion ck_transacciones_coherencia.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    /** Solo en las transferencias: la cuenta que recibe el dinero. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_destino_id")
    private Cuenta cuentaDestino;

    // 13 caracteres para que quepa TRANSFERENCIA. En la base ya era VARCHAR(10)
    // y se amplia en la V8.
    @Column(nullable = false, length = 13)
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

    /**
     * RF-045. Transferencia entre dos cuentas propias.
     *
     * Constructor aparte y no un parametro mas del otro: una transferencia no
     * tiene categoria y si tiene destino, justo al contrario que un ingreso o un
     * gasto. Con un solo constructor habria que pasar nulos y recordar cual toca
     * en cada caso.
     */
    public static Transaccion transferencia(Usuario usuario, Cuenta origen, Cuenta destino,
                                            BigDecimal monto, LocalDate fecha,
                                            String descripcion) {
        Transaccion t = new Transaccion();
        t.usuario = usuario;
        t.cuenta = origen;
        t.cuentaDestino = destino;
        t.categoria = null;
        t.tipo = TRANSFERENCIA;
        t.monto = monto;
        t.fecha = fecha;
        t.descripcion = descripcion;
        t.fechaRegistro = LocalDateTime.now();
        return t;
    }

    public boolean esIngreso() {
        return INGRESO.equals(tipo);
    }

    public boolean esTransferencia() {
        return TRANSFERENCIA.equals(tipo);
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
    public Cuenta getCuentaDestino() { return cuentaDestino; }
    public Categoria getCategoria() { return categoria; }
    public String getTipo() { return tipo; }
    public BigDecimal getMonto() { return monto; }
    public LocalDate getFecha() { return fecha; }
    public String getDescripcion() { return descripcion; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
}
