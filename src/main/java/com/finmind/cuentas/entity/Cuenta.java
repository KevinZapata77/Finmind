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

    /** La que se crea sola al registrarse: el punto de partida de todos. */
    public static final String EFECTIVO = "EFECTIVO";

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

    /**
     * Cupo total de la tarjeta (RF-043). Nulo en los demas tipos de cuenta y
     * tambien en una tarjeta cuyo cupo el usuario no haya registrado todavia.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal cupo;

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

    /** RN-020: una tarjeta de credito es deuda, no dinero disponible. */
    public boolean esPasivo() {
        return TARJETA_CREDITO.equals(tipo);
    }

    /**
     * Saldo actual a partir del movimiento neto de la cuenta.
     *
     * RN-021. El signo depende del tipo, y esta es la correccion del defecto
     * DEF-13:
     *
     *   Cuenta normal   saldo = inicial + ingresos - gastos
     *                   gastar reduce lo que tienes.
     *
     *   Tarjeta         saldo = inicial + gastos - ingresos
     *                   comprar AUMENTA lo que debes, y un ingreso a la tarjeta
     *                   es un pago que lo reduce.
     *
     * Antes se aplicaba la primera formula a todo. En una tarjeta eso hacia que
     * comprar bajara la deuda, y con suficientes compras el saldo llegaba a
     * negativo: la aplicacion terminaba afirmando que el banco le debia al
     * usuario.
     *
     * El calculo vive aqui y no en el servicio para que exista un solo lugar
     * donde el signo se decide. Repartido entre el servicio, los reportes y el
     * cliente, bastaba con olvidarlo en uno para volver a tener dos verdades.
     *
     * @param neto ingresos menos gastos de la cuenta, tal como lo devuelve la base
     */
    public BigDecimal saldoCon(BigDecimal neto) {
        BigDecimal movimiento = neto == null ? BigDecimal.ZERO : neto;
        return esPasivo()
                ? saldoInicial.subtract(movimiento)
                : saldoInicial.add(movimiento);
    }

    /**
     * Cuanto queda por gastar de la tarjeta. Nulo si no es tarjeta o si el
     * usuario no registro el cupo.
     *
     * Puede dar negativo, y se deja: significa que la deuda supero el cupo, que
     * es algo que pasa de verdad. La aplicacion registra lo que ocurrio, no lo
     * que deberia haber ocurrido, asi que no se recorta a cero ni se bloquea el
     * gasto. El cliente lo muestra como aviso.
     */
    public BigDecimal cupoDisponibleCon(BigDecimal saldoActual) {
        if (!esPasivo() || cupo == null) return null;
        return cupo.subtract(saldoActual);
    }

    public void editar(String nombre, String tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }

    /** El cupo se ajusta aparte del resto: cambia con mas frecuencia. */
    public void cambiarCupo(BigDecimal cupo) {
        this.cupo = cupo;
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
    public BigDecimal getCupo() { return cupo; }
    public String getMoneda() { return moneda; }
    public Boolean getActiva() { return activa; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
