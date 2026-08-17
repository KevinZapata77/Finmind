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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Balance, composicion del gasto y panel (RF-021, RF-022).
 *
 * Este servicio no tiene tabla propia: solo lee lo que ya registraron los
 * demas modulos. Por eso no hay entidad ni migracion asociadas.
 */
@Service
public class ServicioReportes {

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
                obligaciones.patrimonio(usuarioId, cuentas.totalActivos(usuarioId)),
                enAlerta);
    }

    private LocalDate[] rangoDelMes(Short anio, Short mes) {
        YearMonth actual = YearMonth.now();
        YearMonth periodo = YearMonth.of(
                anio != null ? anio : actual.getYear(),
                mes != null ? mes : actual.getMonthValue());
        return new LocalDate[]{periodo.atDay(1), periodo.atEndOfMonth()};
    }
}
