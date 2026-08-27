package com.finmind.alertas.service;

import com.finmind.alertas.dto.AlertaResponse;
import com.finmind.alertas.dto.ResumenAlertasResponse;
import com.finmind.cuentas.dto.CuentaResponse;
import com.finmind.cuentas.service.ServicioCuentas;
import com.finmind.fijos.dto.GastoFijoResponse;
import com.finmind.fijos.service.ServicioGastosFijos;
import com.finmind.movimientos.entity.Transaccion;
import com.finmind.movimientos.repository.TransaccionRepository;
import com.finmind.obligaciones.dto.ObligacionResponse;
import com.finmind.obligaciones.service.ServicioObligaciones;
import com.finmind.presupuestos.dto.PresupuestoResponse;
import com.finmind.presupuestos.service.ServicioPresupuestos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * RF-047. Avisos sobre como va el mes.
 *
 * POR QUE NO HAY TABLA DE ALERTAS
 * Una alerta no es un dato: es una conclusion sobre datos que ya existen. Se
 * calcula cada vez que se pide. Guardarlas obligaria a recalcularlas con cada
 * movimiento nuevo y, si algo fallara, el usuario veria un aviso que ya no es
 * cierto — "no te alcanza" el dia despues de cobrar. Es el mismo motivo por el
 * que los saldos tampoco se guardan.
 *
 * LO QUE HACE POSIBLE EL AVISO
 * Sin el modulo de gastos fijos esto no se podria hacer. La aplicacion sabria
 * cuanto se gasto, pero no que compromisos vienen, y sin eso no hay forma de
 * decir si el dinero alcanza. Anticipar exige conocer el futuro comprometido.
 */
@Service
public class ServicioAlertas {

    /** Severidades. Ordenan la lista: lo grave arriba. */
    private static final String ALTA = "ALTA";
    private static final String MEDIA = "MEDIA";
    private static final String BAJA = "BAJA";

    /**
     * Antes del quinto dia no se proyecta.
     *
     * Con dos dias de datos, un mercado grande dispara el ritmo diario y la
     * proyeccion diria que el usuario va a gastar tres veces su sueldo. Un aviso
     * falso al principio de mes ensena al usuario a ignorar todos los demas.
     */
    private static final int DIAS_MINIMOS_PARA_PROYECTAR = 5;

    /** A partir de este consumo del cupo se avisa. */
    private static final BigDecimal UMBRAL_CUPO = new BigDecimal("0.85");

    private final TransaccionRepository movimientos;
    private final ServicioGastosFijos fijos;
    private final ServicioPresupuestos presupuestos;
    private final ServicioObligaciones obligaciones;
    private final ServicioCuentas cuentas;

    public ServicioAlertas(TransaccionRepository movimientos,
                           ServicioGastosFijos fijos,
                           ServicioPresupuestos presupuestos,
                           ServicioObligaciones obligaciones,
                           ServicioCuentas cuentas) {
        this.movimientos = movimientos;
        this.fijos = fijos;
        this.presupuestos = presupuestos;
        this.obligaciones = obligaciones;
        this.cuentas = cuentas;
    }

    @Transactional(readOnly = true)
    public ResumenAlertasResponse delMes(Long usuarioId) {
        LocalDate hoy = LocalDate.now();
        YearMonth mes = YearMonth.from(hoy);
        LocalDate primero = mes.atDay(1);
        LocalDate ultimo = mes.atEndOfMonth();

        int diasDelMes = mes.lengthOfMonth();
        int diasTranscurridos = hoy.getDayOfMonth();

        BigDecimal ingresos = movimientos.totalPorTipo(
                usuarioId, Transaccion.INGRESO, primero, ultimo);
        BigDecimal gastado = movimientos.totalPorTipo(
                usuarioId, Transaccion.GASTO, primero, ultimo);

        BigDecimal ritmo = gastado.divide(
                BigDecimal.valueOf(diasTranscurridos), 2, RoundingMode.HALF_UP);
        BigDecimal proyeccion = ritmo.multiply(BigDecimal.valueOf(diasDelMes))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal pendientes = fijos.pendienteDelMes(usuarioId, mes);
        BigDecimal disponible = ingresos.subtract(gastado);
        BigDecimal holgura = disponible.subtract(pendientes);

        List<AlertaResponse> alertas = new ArrayList<>();
        agregarAlertaDeCompromisos(alertas, usuarioId, mes, disponible, pendientes, holgura);
        agregarAlertaDeRitmo(alertas, diasTranscurridos, ingresos, proyeccion);
        agregarAlertasDePresupuesto(alertas, usuarioId, mes, diasTranscurridos, diasDelMes);
        agregarAlertasDeCuotas(alertas, usuarioId);
        agregarAlertasDeCupo(alertas, usuarioId);

        return new ResumenAlertasResponse(
                alertas, diasTranscurridos, diasDelMes, ingresos, gastado,
                ritmo, proyeccion, pendientes, disponible, holgura,
                lecturaDelMes(holgura, pendientes));
    }

    // ------------------------------------------------------------ cada aviso

    /**
     * RN-027. El aviso central: lo que queda no cubre lo que falta por pagar.
     *
     * Se nombran los compromisos concretos. "No te alcanza" es inutil; "no te
     * alcanza para el arriendo y el internet" le dice al usuario que decidir.
     */
    private void agregarAlertaDeCompromisos(List<AlertaResponse> alertas, Long usuarioId,
                                            YearMonth mes, BigDecimal disponible,
                                            BigDecimal pendientes, BigDecimal holgura) {
        if (pendientes.signum() == 0 || holgura.signum() >= 0) return;

        List<GastoFijoResponse> lista = fijos.pendientesDelMes(usuarioId, mes);
        String nombres = lista.stream()
                .limit(3)
                .map(GastoFijoResponse::nombre)
                .reduce((a, b) -> a + ", " + b)
                .orElse("tus compromisos");
        if (lista.size() > 3) nombres += " y " + (lista.size() - 3) + " mas";

        alertas.add(AlertaResponse.de("FALTA_PARA_COMPROMISOS", ALTA,
                "No te alcanza para lo que falta del mes",
                "Te quedan " + pesos(disponible) + " y todavia debes pagar " + pesos(pendientes)
                        + " en " + nombres + ". Te faltan " + pesos(holgura.abs()) + ".",
                holgura.abs(), "/gastos-fijos"));
    }

    /**
     * RN-028. Al ritmo actual, el gasto del mes supera lo que entro.
     *
     * Es distinto del aviso anterior: aqui todavia alcanza, pero si el usuario
     * sigue igual no va a alcanzar. Por eso es MEDIA y no ALTA.
     */
    private void agregarAlertaDeRitmo(List<AlertaResponse> alertas, int diasTranscurridos,
                                      BigDecimal ingresos, BigDecimal proyeccion) {
        if (diasTranscurridos < DIAS_MINIMOS_PARA_PROYECTAR) return;
        if (ingresos.signum() == 0 || proyeccion.compareTo(ingresos) <= 0) return;

        alertas.add(AlertaResponse.de("RITMO_ALTO", MEDIA,
                "Vas gastando mas rapido de lo que ingresas",
                "Al ritmo de estos " + diasTranscurridos + " dias cerrarias el mes en "
                        + pesos(proyeccion) + ", y este mes te entraron " + pesos(ingresos) + ".",
                proyeccion.subtract(ingresos), "/movimientos"));
    }

    /**
     * RN-029. Un presupuesto que se va a exceder ANTES de que se exceda.
     *
     * El aviso que ya existia llegaba al 80% de consumo. Este llega antes: si en
     * seis dias se gasto la mitad del mes, el presupuesto no aguanta, aunque
     * todavia vaya en 50%.
     */
    private void agregarAlertasDePresupuesto(List<AlertaResponse> alertas, Long usuarioId,
                                             YearMonth mes, int diasTranscurridos,
                                             int diasDelMes) {
        if (diasTranscurridos < DIAS_MINIMOS_PARA_PROYECTAR) return;

        for (PresupuestoResponse p : presupuestos.listar(
                usuarioId, (short) mes.getYear(), (short) mes.getMonthValue())) {

            if (!Boolean.TRUE.equals(p.activo()) || p.consumo().signum() == 0) continue;
            // Los que ya se excedieron los avisa el modulo de presupuestos.
            if ("EXCEDIDO".equals(p.estado())) continue;

            BigDecimal proyeccion = p.consumo()
                    .divide(BigDecimal.valueOf(diasTranscurridos), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(diasDelMes))
                    .setScale(2, RoundingMode.HALF_UP);

            if (proyeccion.compareTo(p.montoLimite()) > 0) {
                alertas.add(AlertaResponse.de("PRESUPUESTO_EN_RIESGO", MEDIA,
                        "Vas a pasarte en " + p.categoriaNombre(),
                        "Llevas " + pesos(p.consumo()) + " de " + pesos(p.montoLimite())
                                + ". A este ritmo terminarias en " + pesos(proyeccion) + ".",
                        proyeccion.subtract(p.montoLimite()), "/presupuestos"));
            }
        }
    }

    /** RN-030. Cuotas de creditos que vencen pronto. */
    private void agregarAlertasDeCuotas(List<AlertaResponse> alertas, Long usuarioId) {
        for (ObligacionResponse o : obligaciones.listar(usuarioId, true)) {
            if (!o.venceEnSieteDias()) continue;
            alertas.add(AlertaResponse.de("CUOTA_PROXIMA", MEDIA,
                    "Se acerca la cuota de " + o.nombre(),
                    "Tienes que pagar " + pesos(o.cuotaMensual()) + " el " + o.proximoPago() + ".",
                    o.cuotaMensual(), "/obligaciones"));
        }
    }

    /** RN-031. Tarjetas con el cupo casi agotado. */
    private void agregarAlertasDeCupo(List<AlertaResponse> alertas, Long usuarioId) {
        for (CuentaResponse c : cuentas.listar(usuarioId, false)) {
            if (!Boolean.TRUE.equals(c.esPasivo()) || c.cupo() == null) continue;

            BigDecimal usado = c.saldoActual().divide(c.cupo(), 4, RoundingMode.HALF_UP);
            if (usado.compareTo(UMBRAL_CUPO) < 0) continue;

            boolean pasado = c.cupoDisponible().signum() < 0;
            alertas.add(AlertaResponse.de("CUPO_CASI_AGOTADO", pasado ? ALTA : BAJA,
                    pasado ? "Te pasaste del cupo de " + c.nombre()
                           : "Casi sin cupo en " + c.nombre(),
                    pasado
                        ? "Debes " + pesos(c.saldoActual()) + " y tu cupo es " + pesos(c.cupo()) + "."
                        : "Te quedan " + pesos(c.cupoDisponible()) + " de " + pesos(c.cupo()) + ".",
                    c.cupoDisponible(), "/cuentas"));
        }
    }

    // ------------------------------------------------------------------ apoyo

    /**
     * Frase que resume el mes. Existe por la misma razon que en el patrimonio:
     * el estado no puede depender solo del color (RNF-008).
     */
    private String lecturaDelMes(BigDecimal holgura, BigDecimal pendientes) {
        if (pendientes.signum() == 0) {
            return "No tienes compromisos fijos pendientes este mes.";
        }
        if (holgura.signum() < 0) {
            return "Con lo que te queda no cubres los compromisos que faltan.";
        }
        if (holgura.signum() == 0) {
            return "Lo que te queda alcanza justo para tus compromisos.";
        }
        return "Cubres tus compromisos y te sobran " + pesos(holgura) + ".";
    }

    /** Formato corto y sin decimales: en un aviso los centavos estorban. */
    private String pesos(BigDecimal v) {
        return "$" + v.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
