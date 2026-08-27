package com.finmind.reportes.service;

import com.finmind.cuentas.service.ServicioCuentas;
import com.finmind.movimientos.entity.Transaccion;
import com.finmind.movimientos.repository.TransaccionRepository;
import com.finmind.obligaciones.service.ServicioObligaciones;
import com.finmind.presupuestos.dto.PresupuestoResponse;
import com.finmind.presupuestos.service.ServicioPresupuestos;
import com.finmind.reportes.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Balance, composicion del gasto y panel (RF-021, RF-022).
 *
 * Este servicio no tiene tabla propia: solo lee lo que ya registraron los
 * demas modulos. Por eso no hay entidad ni migracion asociadas.
 */
@Service
public class ServicioReportes {

    /**
     * Mismo umbral que ServicioAlertas: la proyeccion no se dibuja antes del
     * quinto dia. Si los dos numeros se separaran, el grafico y el aviso
     * dirian cosas distintas sobre el mismo mes.
     */
    private static final int DIAS_MINIMOS_PARA_PROYECTAR = 5;

    private final TransaccionRepository movimientos;
    private final ServicioPresupuestos presupuestos;
    private final ServicioObligaciones obligaciones;
    private final ServicioCuentas cuentas;

    public ServicioReportes(TransaccionRepository movimientos,
                            ServicioPresupuestos presupuestos,
                            ServicioObligaciones obligaciones,
                            ServicioCuentas cuentas) {
        this.movimientos = movimientos;
        this.presupuestos = presupuestos;
        this.obligaciones = obligaciones;
        this.cuentas = cuentas;
    }

    /** RF-021. Sin parametros, el mes en curso. */
    @Transactional(readOnly = true)
    public BalanceResponse balance(Long usuarioId, Short anio, Short mes) {
        LocalDate[] rango = rangoDelMes(anio, mes);
        BigDecimal ingresos = movimientos.totalPorTipo(usuarioId, Transaccion.INGRESO, rango[0], rango[1]);
        BigDecimal gastos = movimientos.totalPorTipo(usuarioId, Transaccion.GASTO, rango[0], rango[1]);
        return BalanceResponse.de(rango[0], rango[1], ingresos, gastos);
    }

    /** RF-022. */
    @Transactional(readOnly = true)
    public ComposicionResponse gastoPorCategoria(Long usuarioId, Short anio, Short mes) {
        LocalDate[] rango = rangoDelMes(anio, mes);
        return ComposicionResponse.de(
                movimientos.agruparPorCategoria(usuarioId, Transaccion.GASTO, rango[0], rango[1]));
    }

    /** El panel completo en una sola llamada. */
    @Transactional(readOnly = true)
    public PanelResponse panel(Long usuarioId, Short anio, Short mes) {
        // Solo los presupuestos que piden atencion: mostrar los quince en el panel
        // haria que el usuario deje de mirarlos.
        List<PresupuestoResponse> enAlerta = presupuestos.listar(usuarioId, anio, mes).stream()
                .filter(p -> Boolean.TRUE.equals(p.activo()))
                .filter(p -> !"EN_CURSO".equals(p.estado()))
                .toList();

        return new PanelResponse(
                balance(usuarioId, anio, mes),
                gastoPorCategoria(usuarioId, anio, mes),
                obligaciones.patrimonio(usuarioId,
                        cuentas.totalActivos(usuarioId),
                        cuentas.totalDeudaEnTarjetas(usuarioId)),
                enAlerta);
    }

    /**
     * Lo de hoy, la semana y el mes en una sola consulta por periodo.
     * La semana empieza el lunes, que es como cuenta la mayoria en Colombia.
     */
    @Transactional(readOnly = true)
    public ResumenRapidoResponse resumenRapido(Long usuarioId) {
        LocalDate hoy = LocalDate.now();
        LocalDate lunes = hoy.with(java.time.DayOfWeek.MONDAY);
        LocalDate primeroDelMes = hoy.withDayOfMonth(1);

        return new ResumenRapidoResponse(
                periodo(usuarioId, hoy, hoy),
                periodo(usuarioId, lunes, hoy),
                periodo(usuarioId, primeroDelMes, hoy));
    }

    private ResumenRapidoResponse.Periodo periodo(Long usuarioId, LocalDate desde, LocalDate hasta) {
        return ResumenRapidoResponse.Periodo.de(
                movimientos.totalPorTipo(usuarioId, Transaccion.INGRESO, desde, hasta),
                movimientos.totalPorTipo(usuarioId, Transaccion.GASTO, desde, hasta));
    }

    /**
     * RF-048. El acumulado dia por dia, para poder trazar la curva del mes.
     *
     * El corte es hoy cuando se pide el mes en curso, y el fin de mes cuando ya
     * paso. Dibujar la linea hasta el dia 31 en un mes que va por el 9 pintaria
     * veintidos dias planos y daria a entender que el usuario dejo de gastar.
     */
    @Transactional(readOnly = true)
    public RitmoResponse ritmo(Long usuarioId, Short anio, Short mes) {
        YearMonth periodo = mesPedido(anio, mes);
        LocalDate primero = periodo.atDay(1);
        LocalDate hoy = LocalDate.now();

        boolean esMesEnCurso = periodo.equals(YearMonth.from(hoy));
        LocalDate corte = esMesEnCurso ? hoy : periodo.atEndOfMonth();
        // Un mes futuro no tiene nada que mostrar: el corte quedaria antes del
        // dia 1 y el bucle no daria ni una vuelta.
        if (corte.isBefore(primero)) {
            return new RitmoResponse(List.of(), periodo.lengthOfMonth(), 0,
                    null, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // Los dias sin movimientos no vienen de la consulta; el mapa los deja
        // en cero y el acumulado de abajo se encarga de arrastrar el anterior.
        Map<Integer, BigDecimal> ingresoDelDia = new HashMap<>();
        Map<Integer, BigDecimal> gastoDelDia = new HashMap<>();
        for (Object[] fila : movimientos.agruparPorDia(usuarioId, primero, corte)) {
            int dia = ((LocalDate) fila[0]).getDayOfMonth();
            BigDecimal monto = (BigDecimal) fila[2];
            if (Transaccion.INGRESO.equals(fila[1])) {
                ingresoDelDia.merge(dia, monto, BigDecimal::add);
            } else {
                gastoDelDia.merge(dia, monto, BigDecimal::add);
            }
        }

        List<RitmoResponse.Dia> dias = new ArrayList<>();
        BigDecimal ingresoAcumulado = BigDecimal.ZERO;
        BigDecimal gastoAcumulado = BigDecimal.ZERO;
        for (int dia = 1; dia <= corte.getDayOfMonth(); dia++) {
            ingresoAcumulado = ingresoAcumulado.add(
                    ingresoDelDia.getOrDefault(dia, BigDecimal.ZERO));
            gastoAcumulado = gastoAcumulado.add(
                    gastoDelDia.getOrDefault(dia, BigDecimal.ZERO));
            dias.add(new RitmoResponse.Dia(dia, ingresoAcumulado, gastoAcumulado));
        }

        // Solo se proyecta el mes que esta corriendo, y con el mismo criterio de
        // las alertas: antes del dia 5 la proyeccion multiplicaria una sola
        // compra grande por todo el mes.
        BigDecimal proyeccion = null;
        if (esMesEnCurso && corte.getDayOfMonth() >= DIAS_MINIMOS_PARA_PROYECTAR) {
            proyeccion = gastoAcumulado
                    .divide(BigDecimal.valueOf(corte.getDayOfMonth()), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(periodo.lengthOfMonth()))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new RitmoResponse(dias, periodo.lengthOfMonth(), corte.getDayOfMonth(),
                proyeccion, ingresoAcumulado, gastoAcumulado);
    }

    private YearMonth mesPedido(Short anio, Short mes) {
        YearMonth actual = YearMonth.now();
        return YearMonth.of(anio != null ? anio : actual.getYear(),
                mes != null ? mes : actual.getMonthValue());
    }

    private LocalDate[] rangoDelMes(Short anio, Short mes) {
        YearMonth actual = YearMonth.now();
        YearMonth periodo = YearMonth.of(
                anio != null ? anio : actual.getYear(),
                mes != null ? mes : actual.getMonthValue());
        return new LocalDate[]{periodo.atDay(1), periodo.atEndOfMonth()};
    }
}
