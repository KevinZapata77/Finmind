package com.finmind.fijos.entity;

import com.finmind.categorias.entity.Categoria;
import com.finmind.usuarios.entity.Usuario;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Set;

/**
 * RF-046. Compromiso que se repite: arriendo, servicios, una suscripcion.
 *
 * QUE ES Y QUE NO ES
 * Un gasto fijo es la INTENCION de un pago que vuelve cada periodo. No es un
 * movimiento. Cuando el pago ocurre de verdad, el usuario lo registra como
 * transaccion igual que cualquier otro gasto. Si el gasto fijo tambien contara
 * como movimiento, el mismo dinero se restaria dos veces del balance.
 *
 * Existe porque sin el la aplicacion no puede anticipar nada. Sabe en que se
 * gasto, pero no que viene: para decir "no te alcanza para el arriendo" hay que
 * saber que el arriendo son 1.100.000 el dia 2 de cada mes.
 */
@Entity
@Table(name = "gastos_fijos")
public class GastoFijo {

    public static final Set<String> PERIODICIDADES = Set.of("MENSUAL", "QUINCENAL", "SEMANAL");

    /**
     * Semanas promedio de un mes: 365 / 12 / 7 = 4,345.
     *
     * Se usa para llevar un gasto semanal a su equivalente mensual. No es
     * exacto — ningun mes tiene 4,345 semanas — y por eso el numero se
     * aproxima. La alternativa seria contar los lunes de cada mes, que da un
     * resultado que salta entre 4 y 5 y hace que la proyeccion cambie de mes a
     * mes sin que el usuario haya cambiado nada.
     */
    private static final BigDecimal SEMANAS_POR_MES = new BigDecimal("4.345");

    private static final BigDecimal QUINCENAS_POR_MES = new BigDecimal("2");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * La categoria es obligatoria: es lo que permite saber si el compromiso ya
     * se pago este mes, comparandolo con lo gastado en esa categoria.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 10)
    private String periodicidad;

    /**
     * En MENSUAL y QUINCENAL, el dia del mes (1-28). En SEMANAL, el dia de
     * la semana (1 lunes, ..., 7 domingo, ISO-8601). Se guarda en la misma
     * columna en los dos casos porque en ninguno de los dos se pisan los
     * rangos: 1-7 cabe dentro de 1-28, y la columna en la base ya lo permite.
     */
    @Column(name = "dia_pago", nullable = false)
    private Short diaPago;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    protected GastoFijo() {
        // Requerido por JPA.
    }

    public GastoFijo(Usuario usuario, Categoria categoria, String nombre,
                     BigDecimal monto, String periodicidad, Short diaPago) {
        this.usuario = usuario;
        this.categoria = categoria;
        this.nombre = nombre;
        this.monto = monto;
        this.periodicidad = periodicidad;
        this.diaPago = diaPago;
        this.activo = Boolean.TRUE;
        this.fechaCreacion = LocalDateTime.now();
    }

    /**
     * RN-025. Cuanto pesa este compromiso en un mes.
     *
     * Un gasto semanal de 50.000 no son 50.000 al mes: son unos 217.000. Sin
     * esta conversion, un usuario con cinco compromisos semanales veria una
     * proyeccion que subestima su gasto real por cuatro.
     */
    public BigDecimal montoMensualEquivalente() {
        return switch (periodicidad) {
            case "SEMANAL" -> monto.multiply(SEMANAS_POR_MES).setScale(2, RoundingMode.HALF_UP);
            case "QUINCENAL" -> monto.multiply(QUINCENAS_POR_MES).setScale(2, RoundingMode.HALF_UP);
            default -> monto.setScale(2, RoundingMode.HALF_UP);
        };
    }

    /**
     * Proxima fecha de pago a partir de hoy.
     *
     * En los semanales, diaPago es el dia de la semana: 1 lunes, ..., 7
     * domingo (ISO-8601, igual que DayOfWeek). Se busca la primera fecha
     * desde hoy —hoy incluido— que caiga en ese dia.
     *
     * DEFECTO CORREGIDO (DEF-19): esta rama nunca uso diaPago. Calculaba
     * hoy.getDayOfMonth() % 7, que es el resto del DIA DEL MES entre 7 y no
     * tiene ninguna relacion con el dia de la semana. Un compromiso marcado
     * "cada viernes" caia en cualquier dia: el numero cambiaba solo porque
     * cambiaba el dia del mes, no porque el dia de la semana fuera otro.
     *
     * En los mensuales es el dia pactado de este mes si aun no paso, y si ya
     * paso el del mes siguiente. En quincenales el primer pago es el dia 15
     * y el segundo el dia pactado del mes siguiente.
     */
    public LocalDate proximoPagoDesde(LocalDate hoy) {
        return switch (periodicidad) {
            case "SEMANAL" -> {
                DayOfWeek objetivo = DayOfWeek.of(diaPago);
                int distancia = objetivo.getValue() - hoy.getDayOfWeek().getValue();
                // Modulo que nunca da negativo: si el dia ya paso esta semana,
                // salta a la semana siguiente. Si es hoy, distancia es 0 y el
                // pago es hoy mismo — no ha pasado, sigue siendo el proximo.
                yield hoy.plusDays(distancia < 0 ? distancia + 7 : distancia);
            }

            case "QUINCENAL" -> hoy.getDayOfMonth() < 15
                    ? hoy.withDayOfMonth(15)
                    : hoy.plusMonths(1).withDayOfMonth(Math.min(diaPago, 28));

            default -> {
                LocalDate esteMes = hoy.withDayOfMonth(diaPago);
                yield esteMes.isBefore(hoy) ? esteMes.plusMonths(1) : esteMes;
            }
        };
    }

    /** Si el proximo pago cae dentro del mes que se esta mirando. */
    public boolean venceEn(YearMonth mes, LocalDate hoy) {
        return YearMonth.from(proximoPagoDesde(hoy)).equals(mes);
    }

    public boolean perteneceA(Long usuarioId) {
        return usuario != null && usuario.getId().equals(usuarioId);
    }

    public boolean estaActivo() {
        return Boolean.TRUE.equals(activo);
    }

    public void editar(Categoria categoria, String nombre, BigDecimal monto,
                       String periodicidad, Short diaPago) {
        this.categoria = categoria;
        this.nombre = nombre;
        this.monto = monto;
        this.periodicidad = periodicidad;
        this.diaPago = diaPago;
    }

    public void desactivar() { this.activo = Boolean.FALSE; }
    public void activar() { this.activo = Boolean.TRUE; }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Categoria getCategoria() { return categoria; }
    public String getNombre() { return nombre; }
    public BigDecimal getMonto() { return monto; }
    public String getPeriodicidad() { return periodicidad; }
    public Short getDiaPago() { return diaPago; }
    public Boolean getActivo() { return activo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
