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
import java.util.LinkedHashMap;
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

    /**
     * RF-050. Seis meses por defecto: es el minimo para que se vea una
     * tendencia y no un zigzag. Con tres, un mes atipico —diciembre, unas
     * vacaciones— parece la nueva normalidad.
     */
    private static final int MESES_DE_HISTORICO = 6;

    /**
     * Tope duro. El parametro llega de la URL y sin limite alguien puede pedir
     * mil meses, que son ochenta y tres anios de filas por una sola peticion.
     */
    private static final int MAXIMO_MESES_DE_HISTORICO = 24;

    /**
     * Los nombres se escriben aqui y no se sacan de un Locale del sistema: el
     * servidor puede estar configurado en ingles y el usuario leeria "August".
     */
    private static final String[] NOMBRES_DE_MES = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};

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

    /**
     * RF-022 y RF-050. La composicion del gasto, comparada con el mes anterior.
     *
     * La comparacion se agrega aqui porque es la cifra que mas mueve a la gente:
     * "gastaste 80.000 mas en Alimentacion que en julio" dice mas que cualquier
     * total, porque senala una causa y algo que se puede cambiar. Cuesta una
     * segunda consulta del mismo tipo, ya agrupada por la base.
     */
    @Transactional(readOnly = true)
    public ComposicionResponse gastoPorCategoria(Long usuarioId, Short anio, Short mes) {
        YearMonth periodo = mesPedido(anio, mes);
        YearMonth previo = periodo.minusMonths(1);

        return ComposicionResponse.de(
                movimientos.agruparPorCategoria(usuarioId, Transaccion.GASTO,
                        periodo.atDay(1), periodo.atEndOfMonth()),
                movimientos.agruparPorCategoria(usuarioId, Transaccion.GASTO,
                        previo.atDay(1), finDelTramoAComparar(periodo, previo)));
    }

    /**
     * RN-032. Hasta que dia del mes anterior se mide, para que la comparacion
     * sea justa.
     *
     * Si se esta mirando el mes EN CURSO, el mes anterior se recorta al dia de
     * hoy: comparar nueve dias contra treinta diria siempre que el usuario
     * gasta menos, y lo diria todos los meses. Si se esta mirando un mes ya
     * cerrado, los dos meses estan completos y se comparan enteros.
     */
    private LocalDate finDelTramoAComparar(YearMonth periodo, YearMonth previo) {
        LocalDate hoy = LocalDate.now();
        if (!periodo.equals(YearMonth.from(hoy))) {
            return previo.atEndOfMonth();
        }
        // atDay falla con un dia que el mes no tiene: hoy 31, mes anterior de 30.
        return previo.atDay(Math.min(hoy.getDayOfMonth(), previo.lengthOfMonth()));
    }

    /**
     * RF-050. Ingresos y gastos de los ultimos `meses` meses, incluido el actual.
     *
     * POR QUE SE REUSA agruparPorDia Y NO SE ESCRIBE UN GROUP BY POR MES
     * Agrupar por anio/mes en JPQL obliga a usar funciones de fecha, y ahi H2
     * (las pruebas) y PostgreSQL (produccion) no se escriben igual: seria una
     * consulta que pasa las pruebas y falla en el servidor, que es el peor tipo
     * de consulta. Se pide el rango completo ya sumado por dia —una sola
     * consulta— y se reparte por mes en Java, que se comporta igual en los dos
     * lados. Son como maximo 180 filas: agrupar eso en memoria no es un costo.
     */
    @Transactional(readOnly = true)
    public HistoricoResponse historico(Long usuarioId, Integer meses) {
        int cuantos = (meses == null || meses < 1) ? MESES_DE_HISTORICO
                : Math.min(meses, MAXIMO_MESES_DE_HISTORICO);

        YearMonth actual = YearMonth.now();
        YearMonth primero = actual.minusMonths(cuantos - 1L);

        // Acumuladores por mes, ya inicializados en cero: un mes sin ningun
        // movimiento tiene que aparecer en la serie como un mes en cero, no
        // desaparecer. Si desapareciera, la linea uniria dos meses no
        // consecutivos y mentiria sobre la tendencia.
        Map<YearMonth, BigDecimal[]> porMes = new LinkedHashMap<>();
        for (int i = 0; i < cuantos; i++) {
            porMes.put(primero.plusMonths(i), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        for (Object[] fila : movimientos.agruparPorDia(
                usuarioId, primero.atDay(1), actual.atEndOfMonth())) {
            YearMonth delDia = YearMonth.from((LocalDate) fila[0]);
            BigDecimal[] acumulado = porMes.get(delDia);
            if (acumulado == null) continue;  // fuera del rango pedido
            int posicion = Transaccion.INGRESO.equals(fila[1]) ? 0 : 1;
            acumulado[posicion] = acumulado[posicion].add((BigDecimal) fila[2]);
        }

        List<HistoricoResponse.Mes> serie = porMes.entrySet().stream()
                .map(e -> new HistoricoResponse.Mes(
                        e.getKey().getYear(), e.getKey().getMonthValue(),
                        NOMBRES_DE_MES[e.getKey().getMonthValue() - 1],
                        e.getValue()[0], e.getValue()[1],
                        e.getValue()[0].subtract(e.getValue()[1])))
                .toList();

        return new HistoricoResponse(serie, comparacionConElMesAnterior(usuarioId));
    }

    /**
     * RN-032. El mes en curso contra el anterior, midiendo los mismos dias.
     *
     * Comparar el total del mes en curso contra el total del mes anterior seria
     * la version obvia y estaria mal: el dia 5 la cuenta diria siempre que el
     * usuario gasta muchisimo menos, porque compara cinco dias contra treinta.
     * Un numero que siempre tranquiliza es un numero roto.
     */
    private HistoricoResponse.Comparacion comparacionConElMesAnterior(Long usuarioId) {
        LocalDate hoy = LocalDate.now();
        YearMonth actual = YearMonth.from(hoy);
        YearMonth previo = actual.minusMonths(1);
        int corte = hoy.getDayOfMonth();

        // Si hoy es 31 y el mes anterior tuvo 30 dias, el tramo previo se corta
        // en 30: pedir el 31 de un mes que no lo tiene es una fecha inexistente.
        LocalDate finDelTramoPrevio = previo.atDay(Math.min(corte, previo.lengthOfMonth()));

        BigDecimal esteMes = movimientos.totalPorTipo(
                usuarioId, Transaccion.GASTO, actual.atDay(1), hoy);
        BigDecimal mesAnterior = movimientos.totalPorTipo(
                usuarioId, Transaccion.GASTO, previo.atDay(1), finDelTramoPrevio);

        BigDecimal variacion = esteMes.subtract(mesAnterior);

        // Sin gasto en el tramo previo no hay porcentaje posible. La cifra
        // absoluta si, y es la que de verdad se muestra.
        BigDecimal porcentaje = mesAnterior.signum() == 0 ? null
                : variacion.multiply(new BigDecimal("100"))
                        .divide(mesAnterior, 1, RoundingMode.HALF_UP);

        return new HistoricoResponse.Comparacion(
                corte, esteMes, mesAnterior, variacion, porcentaje);
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
