package com.finmind.fijos.entity;

import com.finmind.categorias.entity.Categoria;
import com.finmind.usuarios.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * PRUEBA UNITARIA DE DOMINIO. Sin Spring, sin base de datos, sin Mockito.
 *
 * POR QUE ESTA CLASE EXISTE SI YA HAY 189 PRUEBAS
 * Las otras pruebas de FinMind son de integracion: levantan el contexto de
 * Spring, entran por HTTP con MockMvc y escriben en H2. Comprueban que el
 * sistema completo funciona, y esa es su virtud. Su defecto es que cuando una
 * falla no dicen DONDE esta el error: puede estar en el controlador, en el
 * servicio, en el mapeo de JPA o en la regla de negocio.
 *
 * Aqui se prueba una sola clase, aislada. Si esta prueba falla, el error esta
 * en GastoFijo y en ningun otro sitio. Ademas corre en milisegundos, porque no
 * arranca nada.
 *
 * QUE SE PRUEBA
 * La aritmetica del dinero (RN-025) y el calendario de pagos, que es donde
 * aparecieron tres defectos reales: DEF-19, DEF-20 y DEF-21. Cada uno tiene su
 * prueba, para que no vuelvan.
 *
 * SOBRE LAS FECHAS FIJAS
 * Todas las pruebas usan fechas escritas a mano y nunca LocalDate.now(). Una
 * prueba que depende del dia en que se ejecuta es una prueba que un dia falla
 * sola, y cuando eso pasa el equipo deja de creerle a la suite entera.
 */
@DisplayName("GastoFijo: aritmetica y calendario")
class GastoFijoTest {

    /** Martes. Verificado: 2026-03-10 cae en martes. */
    private static final LocalDate MARTES_10_MARZO = LocalDate.of(2026, 3, 10);

    private static Usuario unUsuario(long id) {
        Usuario u = new Usuario("Kevin", "Zapata", "kevin@finmind.test", "hash", null);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private static Categoria unaCategoria() {
        return new Categoria(null, "Vivienda", Categoria.GASTO, "home", "#2DD4BF");
    }

    private static GastoFijo unGastoFijo(String periodicidad, String monto, int diaPago) {
        return new GastoFijo(unUsuario(1L), unaCategoria(), "Arriendo",
                new BigDecimal(monto), periodicidad, (short) diaPago);
    }

    // ================================================ RN-025: equivalente mensual

    @Test
    @DisplayName("RN-025: un gasto mensual pesa exactamente su monto")
    void mensualPesaSuMonto() {
        assertThat(unGastoFijo("MENSUAL", "1100000", 2).montoMensualEquivalente())
                .isEqualByComparingTo("1100000.00");
    }

    @Test
    @DisplayName("RN-025: un gasto quincenal pesa el doble de su monto")
    void quincenalPesaElDoble() {
        assertThat(unGastoFijo("QUINCENAL", "300000", 5).montoMensualEquivalente())
                .isEqualByComparingTo("600000.00");
    }

    /**
     * Este es el numero que mas confunde al usuario y por eso se prueba con la
     * cifra concreta: 50.000 semanales no son 50.000 al mes, son 217.250.
     * Sin esta conversion, alguien con cinco compromisos semanales veria una
     * proyeccion que subestima su gasto real por cuatro.
     */
    @Test
    @DisplayName("RN-025: un gasto semanal de 50.000 pesa 217.250 al mes")
    void semanalPesaCuatroSemanasYMedia() {
        assertThat(unGastoFijo("SEMANAL", "50000", 5).montoMensualEquivalente())
                .isEqualByComparingTo("217250.00");
    }

    /**
     * 33.333 x 4,345 = 144.831,885. El resultado se guarda en una columna de dos
     * decimales, asi que la tercera cifra tiene que redondearse hacia arriba y
     * no truncarse: la diferencia es de medio centavo por compromiso, pero un
     * redondeo hacia abajo acumulado es la clase de error que despues nadie
     * encuentra.
     */
    @Test
    @DisplayName("RN-025: el equivalente mensual redondea a dos decimales hacia arriba")
    void elEquivalenteRedondeaHaciaArriba() {
        assertThat(unGastoFijo("SEMANAL", "33333", 5).montoMensualEquivalente())
                .isEqualByComparingTo("144831.89");
    }

    // ============================================== DEF-19: el semanal es un dia

    /**
     * DEF-19. Antes esta rama calculaba hoy.getDayOfMonth() % 7, el resto del
     * DIA DEL MES entre siete, que no tiene ninguna relacion con el dia de la
     * semana. Un compromiso "cada viernes" caia en cualquier dia.
     *
     * 2026-03-10 es martes; el proximo viernes es el 13.
     */
    @Test
    @DisplayName("DEF-19: un compromiso semanal cae en el dia de la semana pactado")
    void elSemanalCaeEnElDiaPactado() {
        LocalDate proximo = unGastoFijo("SEMANAL", "50000", 5).proximoPagoDesde(MARTES_10_MARZO);

        assertThat(proximo.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(proximo).isEqualTo(LocalDate.of(2026, 3, 13));
    }

    /** Si el dia pactado es hoy, el pago es hoy. Hoy todavia no ha pasado. */
    @Test
    @DisplayName("DEF-19: si el dia pactado es hoy, el pago es hoy")
    void elSemanalDeHoyEsHoy() {
        LocalDate viernes = LocalDate.of(2026, 3, 13);

        assertThat(unGastoFijo("SEMANAL", "50000", 5).proximoPagoDesde(viernes))
                .isEqualTo(viernes);
    }

    /** Si el dia ya paso esta semana, salta a la siguiente y no al pasado. */
    @Test
    @DisplayName("DEF-19: si el dia ya paso, el pago salta a la semana siguiente")
    void elSemanalSaltaALaSemanaSiguiente() {
        LocalDate sabado = LocalDate.of(2026, 3, 14);

        assertThat(unGastoFijo("SEMANAL", "50000", 5).proximoPagoDesde(sabado))
                .isEqualTo(LocalDate.of(2026, 3, 20));
    }

    // ============================== DEF-20: un dato viejo no puede tumbar la lista

    /**
     * DEF-20. En la base hay compromisos creados cuando SEMANAL aceptaba
     * cualquier dia del 1 al 28, porque el calculo viejo ni siquiera miraba ese
     * campo. Al leerlos, DayOfWeek.of(20) lanzaba DateTimeException y tumbaba el
     * listado entero con un 500: el usuario dejaba de ver TODOS sus compromisos
     * por culpa de uno solo.
     *
     * El constructor se usa aqui directamente, sin pasar por el servicio, justo
     * para reproducir ese dato viejo: hoy el servicio ya no deja guardar un 20 en
     * un semanal, pero en la base ya hay unos cuantos.
     *
     * 20 se envuelve dentro del rango 1..7 y da 6, sabado. Se envuelve en vez de
     * recortar porque recortar amontonaria todos los valores altos en el mismo
     * dia.
     */
    @Test
    @DisplayName("DEF-20: un dia fuera de rango se normaliza y no lanza excepcion")
    void unDiaViejoFueraDeRangoNoRompeLaLectura() {
        GastoFijo viejo = unGastoFijo("SEMANAL", "50000", 20);

        assertThatCode(() -> viejo.proximoPagoDesde(MARTES_10_MARZO)).doesNotThrowAnyException();
        assertThat(viejo.proximoPagoDesde(MARTES_10_MARZO).getDayOfWeek())
                .isEqualTo(DayOfWeek.SATURDAY);
    }

    // ================================== DEF-21: el quincenal paga dos veces al mes

    /**
     * DEF-21. Antes esta rama fijaba el primer pago el dia 15 sin mirar el dia
     * que el usuario habia elegido, y el segundo lo mandaba al MES siguiente.
     * Eso no es quincenal: era un pago mensual con la fecha corrida. Alguien que
     * paga el 5 y el 20 veia una sola fecha al mes, y encima una que no habia
     * pactado. El monto mensual si contaba dos pagos (RN-025), asi que la cifra
     * decia una cosa y el calendario otra.
     */
    @Test
    @DisplayName("DEF-21: el quincenal tiene dos fechas en el mes, la pactada y quince dias despues")
    void elQuincenalTieneDosFechasEnElMes() {
        List<LocalDate> fechas = unGastoFijo("QUINCENAL", "300000", 5)
                .fechasDelMes(YearMonth.of(2026, 3));

        assertThat(fechas).containsExactly(
                LocalDate.of(2026, 3, 5),
                LocalDate.of(2026, 3, 20));
    }

    @Test
    @DisplayName("DEF-21: antes del primer pago, el proximo es el primero")
    void antesDelPrimerPagoTocaElPrimero() {
        assertThat(unGastoFijo("QUINCENAL", "300000", 5)
                .proximoPagoDesde(LocalDate.of(2026, 3, 1)))
                .isEqualTo(LocalDate.of(2026, 3, 5));
    }

    @Test
    @DisplayName("DEF-21: entre los dos pagos, el proximo es el segundo del MISMO mes")
    void entreLosDosPagosTocaElSegundoDelMismoMes() {
        assertThat(unGastoFijo("QUINCENAL", "300000", 5)
                .proximoPagoDesde(MARTES_10_MARZO))
                .isEqualTo(LocalDate.of(2026, 3, 20));
    }

    @Test
    @DisplayName("DEF-21: pasados los dos, el proximo es el primero del mes siguiente")
    void pasadosLosDosTocaElMesSiguiente() {
        assertThat(unGastoFijo("QUINCENAL", "300000", 5)
                .proximoPagoDesde(LocalDate.of(2026, 3, 25)))
                .isEqualTo(LocalDate.of(2026, 4, 5));
    }

    /**
     * DEF-21. Aqui se ve por que el tope del primer pago quincenal es 13 y no 15.
     *
     * Con 13 el segundo pago cae el 28, que existe en todos los meses, incluido
     * febrero. Con 14 el segundo seria el 29 y en un febrero de año comun
     * YearMonth.atDay(29) lanza DateTimeException: la pantalla de gastos fijos se
     * caeria una vez al año y solo para algunos usuarios.
     */
    @Test
    @DisplayName("DEF-21: con el dia 13, el segundo pago cabe hasta en febrero")
    void elTopeDeTreceCabeEnFebrero() {
        List<LocalDate> fechas = unGastoFijo("QUINCENAL", "300000", GastoFijo.MAXIMO_DIA_QUINCENAL)
                .fechasDelMes(YearMonth.of(2026, 2));

        assertThat(fechas).containsExactly(
                LocalDate.of(2026, 2, 13),
                LocalDate.of(2026, 2, 28));
    }

    // ========================================================== MENSUAL y el resto

    @Test
    @DisplayName("El mensual cobra este mes si el dia no ha pasado, y el siguiente si ya paso")
    void elMensualSabeSiElDiaYaPaso() {
        GastoFijo arriendo = unGastoFijo("MENSUAL", "1100000", 2);

        assertThat(arriendo.proximoPagoDesde(LocalDate.of(2026, 3, 1)))
                .isEqualTo(LocalDate.of(2026, 3, 2));
        assertThat(arriendo.proximoPagoDesde(MARTES_10_MARZO))
                .isEqualTo(LocalDate.of(2026, 4, 2));
    }

    @Test
    @DisplayName("El mensual muestra una sola fecha en el mes")
    void elMensualMuestraUnaFecha() {
        assertThat(unGastoFijo("MENSUAL", "1100000", 2).fechasDelMes(YearMonth.of(2026, 3)))
                .containsExactly(LocalDate.of(2026, 3, 2));
    }

    /**
     * En un semanal son cuatro o cinco fechas y enumerarlas no aporta nada sobre
     * "cada viernes". La lista vacia es una decision, no un olvido, y por eso se
     * prueba.
     */
    @Test
    @DisplayName("El semanal no enumera fechas: 'cada viernes' ya lo dice todo")
    void elSemanalNoEnumeraFechas() {
        assertThat(unGastoFijo("SEMANAL", "50000", 5).fechasDelMes(YearMonth.of(2026, 3)))
                .isEmpty();
    }

    @Test
    @DisplayName("venceEn responde si el proximo pago cae dentro del mes que se mira")
    void venceEnMiraElMesDelProximoPago() {
        GastoFijo arriendo = unGastoFijo("MENSUAL", "1100000", 2);

        assertThat(arriendo.venceEn(YearMonth.of(2026, 4), MARTES_10_MARZO)).isTrue();
        assertThat(arriendo.venceEn(YearMonth.of(2026, 3), MARTES_10_MARZO)).isFalse();
    }

    // ==================================================== RN-005: dueño del dato

    @Test
    @DisplayName("RN-005: el compromiso solo pertenece a su dueño")
    void soloPerteneceASuDueno() {
        GastoFijo arriendo = new GastoFijo(unUsuario(7L), unaCategoria(), "Arriendo",
                new BigDecimal("1100000"), "MENSUAL", (short) 2);

        assertThat(arriendo.perteneceA(7L)).isTrue();
        assertThat(arriendo.perteneceA(8L)).isFalse();
    }

    @Test
    @DisplayName("Un compromiso nace activo y desactivarlo no lo borra")
    void naceActivoYSeApaga() {
        GastoFijo arriendo = unGastoFijo("MENSUAL", "1100000", 2);
        assertThat(arriendo.estaActivo()).isTrue();

        arriendo.desactivar();
        assertThat(arriendo.estaActivo()).isFalse();
        assertThat(arriendo.getMonto()).isEqualByComparingTo("1100000");

        arriendo.activar();
        assertThat(arriendo.estaActivo()).isTrue();
    }
}
