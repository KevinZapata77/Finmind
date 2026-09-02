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
import java.util.List;
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

    /** DEF-21. Dias entre los dos pagos de un compromiso quincenal. */
    private static final int DIAS_DE_LA_QUINCENA = 15;

    /**
     * DEF-21. Ultimo dia valido para el PRIMER pago quincenal.
     *
     * El segundo pago es quince dias despues, asi que con 13 el segundo cae
     * como maximo el 28, que existe en todos los meses. Con 14 el segundo
     * caeria el 29 y en febrero no existiria.
     */
    public static final int MAXIMO_DIA_QUINCENAL = 13;

    /**
     * DEF-20. Lleva cualquier valor guardado al rango que la periodicidad
     * admite, sin lanzar excepcion.
     *
     * POR QUE HACE FALTA ESTO Y NO BASTA CON VALIDAR AL GUARDAR
     * La validacion solo protege lo que entra de ahora en adelante. En la base
     * ya hay compromisos creados cuando SEMANAL aceptaba cualquier dia del 1 al
     * 28, porque el calculo viejo ni siquiera miraba este campo. Al leerlos,
     * DayOfWeek.of(20) lanza DateTimeException y tumba el listado entero con un
     * 500: el usuario deja de ver TODOS sus compromisos por culpa de uno solo.
     *
     * Una consulta de lectura no puede fallar por datos que ya estan guardados.
     * La migracion V10 normaliza lo existente; esto es el cinturon por si
     * algun dato se cuela igual.
     */
    private int diaNormalizado(int minimo, int maximo) {
        int valor = diaPago == null ? minimo : diaPago;
        if (valor < minimo) return minimo;
        if (valor > maximo) {
            // Se envuelve en vez de recortar: recortar amontonaria todos los
            // valores altos en el mismo dia. Envolver reparte y es reversible.
            return ((valor - minimo) % (maximo - minimo + 1)) + minimo;
        }
        return valor;
    }

    /**
     * Proxima fecha de pago a partir de hoy.
     *
     * SEMANAL
     * diaPago es el dia de la semana: 1 lunes, ..., 7 domingo (ISO-8601, igual
     * que DayOfWeek). Se busca la primera fecha desde hoy —hoy incluido— que
     * caiga en ese dia.
     *
     * DEF-19: esta rama nunca uso diaPago. Calculaba hoy.getDayOfMonth() % 7,
     * el resto del DIA DEL MES entre 7, que no tiene relacion con el dia de la
     * semana. Un compromiso "cada viernes" caia en cualquier dia.
     *
     * QUINCENAL
     * Dos pagos al mes: el dia pactado y quince dias despues. Se devuelve el
     * primero de los dos que todavia no haya pasado, y si los dos pasaron, el
     * primer pago del mes siguiente.
     *
     * DEF-21: antes esta rama fijaba el primer pago el dia 15 sin mirar el dia
     * que el usuario habia elegido, y el segundo lo mandaba al MES siguiente.
     * Eso no es quincenal: era un pago mensual con la fecha corrida. Alguien
     * que cobra el 5 y el 20 veia una sola fecha al mes, y encima una que no
     * habia pactado. El monto mensual si contaba dos pagos (RN-025), asi que
     * la cifra decia una cosa y el calendario otra.
     *
     * MENSUAL
     * El dia pactado de este mes si aun no paso, y si ya paso el del mes
     * siguiente.
     */
    public LocalDate proximoPagoDesde(LocalDate hoy) {
        return switch (periodicidad) {
            case "SEMANAL" -> {
                DayOfWeek objetivo = DayOfWeek.of(diaNormalizado(1, 7));
                int distancia = objetivo.getValue() - hoy.getDayOfWeek().getValue();
                // Modulo que nunca da negativo: si el dia ya paso esta semana,
                // salta a la semana siguiente. Si es hoy, distancia es 0 y el
                // pago es hoy mismo — no ha pasado, sigue siendo el proximo.
                yield hoy.plusDays(distancia < 0 ? distancia + 7 : distancia);
            }

            case "QUINCENAL" -> {
                int primero = diaNormalizado(1, MAXIMO_DIA_QUINCENAL);
                int segundo = primero + DIAS_DE_LA_QUINCENA;

                LocalDate pagoUno = hoy.withDayOfMonth(primero);
                LocalDate pagoDos = hoy.withDayOfMonth(segundo);

                if (!pagoUno.isBefore(hoy)) yield pagoUno;
                if (!pagoDos.isBefore(hoy)) yield pagoDos;
                yield hoy.plusMonths(1).withDayOfMonth(primero);
            }

            default -> {
                LocalDate esteMes = hoy.withDayOfMonth(diaNormalizado(1, 28));
                yield esteMes.isBefore(hoy) ? esteMes.plusMonths(1) : esteMes;
            }
        };
    }

    /**
     * DEF-21. Las dos fechas de pago del mes que se esta mirando, para poder
     * mostrarlas juntas: un compromiso quincenal con una sola fecha a la vista
     * se lee como mensual.
     */
    public List<LocalDate> fechasDelMes(YearMonth mes) {
        return switch (periodicidad) {
            case "QUINCENAL" -> {
                int primero = diaNormalizado(1, MAXIMO_DIA_QUINCENAL);
                yield List.of(mes.atDay(primero), mes.atDay(primero + DIAS_DE_LA_QUINCENA));
            }
            case "MENSUAL" -> List.of(mes.atDay(diaNormalizado(1, 28)));
            // En semanal son cuatro o cinco fechas y enumerarlas no aporta:
            // "cada viernes" ya lo dice todo.
            default -> List.of();
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
