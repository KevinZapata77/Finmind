package com.finmind.reportes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finmind.identidad.entity.CodigoVerificacion;
import com.finmind.identidad.repository.CodigoVerificacionRepository;
import com.finmind.soporte.LimpiadorDeDatos;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF-050 y RN-032. La serie de varios meses y la comparacion con el mes anterior.
 *
 * QUE SE ESTA PROBANDO Y POR QUE IMPORTA
 * Hasta ahora todos los reportes hablaban de un mes suelto: la aplicacion sabia
 * decir "gastaste 1.200.000 en agosto" pero no "gastaste 300.000 mas que en
 * julio". Estas pruebas cubren lo segundo, que es lo que permite ver que el
 * gasto se esta subiendo antes de que sea un problema.
 *
 * SOBRE LOS MESES QUE SE USAN
 * Se usan meses CERRADOS (el anterior y el trasanterior) para poner los
 * movimientos, porque un mes cerrado tiene todos sus dias: se puede gastar el
 * dia 10 con la seguridad de que ese dia existe. Poner un movimiento "el dia 10
 * de este mes" haria que la prueba fallara del 1 al 9 de cada mes, y una prueba
 * que solo pasa a mitad de mes no prueba nada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Historico de meses y comparacion")
class HistoricoYComparacionTest {

    private static final String CONTRASENA = "ClaveSegura123";
    private static final YearMonth MES_PASADO = YearMonth.now().minusMonths(1);
    private static final YearMonth HACE_DOS_MESES = YearMonth.now().minusMonths(2);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CodigoVerificacionRepository codigoRepository;
    @Autowired private LimpiadorDeDatos limpiador;

    @BeforeEach
    void prepararDatos() {
        limpiador.limpiar();
    }

    // ==================================================== RF-050: la serie

    /**
     * Seis meses por defecto, del mas antiguo al mas reciente. El orden no es
     * un detalle: es el orden en que se dibuja el grafico, y si viniera al
     * reves la tendencia se leeria invertida.
     */
    @Test
    @DisplayName("Por defecto trae 6 meses, del mas antiguo al mas reciente")
    void seisMesesEnOrden() throws Exception {
        String token = usuarioListo("hist@finmind.test");

        JsonNode meses = historico(token, null).get("meses");
        assertThat(meses.size()).isEqualTo(6);

        // El ultimo de la serie es el mes en curso.
        YearMonth actual = YearMonth.now();
        JsonNode ultimo = meses.get(5);
        assertThat(ultimo.get("anio").asInt()).isEqualTo(actual.getYear());
        assertThat(ultimo.get("mes").asInt()).isEqualTo(actual.getMonthValue());

        // Y cada mes es exactamente el siguiente del anterior.
        for (int i = 0; i < meses.size(); i++) {
            YearMonth esperado = actual.minusMonths(5L - i);
            assertThat(YearMonth.of(meses.get(i).get("anio").asInt(),
                    meses.get(i).get("mes").asInt()))
                    .as("posicion %d de la serie", i)
                    .isEqualTo(esperado);
        }
    }

    /**
     * Un mes sin movimientos aparece en cero, no desaparece. Si desapareciera,
     * la linea del grafico uniria dos meses no consecutivos y mentiria sobre la
     * tendencia: un salto de dos meses se veria como un salto de uno.
     */
    @Test
    @DisplayName("Un mes sin movimientos viene en cero, no se omite")
    void losMesesVaciosNoSeOmiten() throws Exception {
        String token = usuarioListo("hist@finmind.test");
        long cuenta = crearCuenta(token);
        long gasto = categoria(token, "GASTO");

        // Solo el mes pasado tiene un gasto; los otros cinco quedan vacios.
        gastarEnFecha(token, cuenta, gasto, "200000.00", MES_PASADO.atDay(10));

        JsonNode meses = historico(token, null).get("meses");
        assertThat(meses.size()).isEqualTo(6);

        int conGasto = 0;
        for (JsonNode m : meses) {
            // Ningun mes viene ausente ni con el campo nulo.
            assertThat(m.get("gastos")).isNotNull();
            if (m.get("gastos").asDouble() > 0) conGasto++;
        }
        assertThat(conGasto).isEqualTo(1);
        assertThat(mesDe(meses, MES_PASADO).get("gastos").asDouble()).isEqualTo(200000.00);
    }

    @Test
    @DisplayName("Cada mes trae ingresos, gastos y su diferencia")
    void cadaMesTraeSuBalance() throws Exception {
        String token = usuarioListo("hist@finmind.test");
        long cuenta = crearCuenta(token);

        registrarEnFecha(token, cuenta, categoria(token, "INGRESO"),
                "3000000.00", MES_PASADO.atDay(3));
        gastarEnFecha(token, cuenta, categoria(token, "GASTO"),
                "1100000.00", MES_PASADO.atDay(8));

        JsonNode mes = mesDe(historico(token, null).get("meses"), MES_PASADO);
        assertThat(mes.get("ingresos").asDouble()).isEqualTo(3000000.00);
        assertThat(mes.get("gastos").asDouble()).isEqualTo(1100000.00);
        assertThat(mes.get("diferencia").asDouble()).isEqualTo(1900000.00);
        // El nombre viene escrito desde el servidor: el cliente no deberia
        // tener que traducir un numero de mes.
        assertThat(mes.get("nombre").asText()).isNotEmpty();
    }

    /** El parametro se respeta, y el tope duro tambien. */
    @Test
    @DisplayName("El numero de meses se puede pedir, con tope de 24")
    void elParametroSeRespetaConTope() throws Exception {
        String token = usuarioListo("hist@finmind.test");

        assertThat(historico(token, 3).get("meses").size()).isEqualTo(3);
        assertThat(historico(token, 12).get("meses").size()).isEqualTo(12);
        // Sin tope, pedir mil meses seria ochenta y tres anios de filas.
        assertThat(historico(token, 500).get("meses").size()).isEqualTo(24);
        // Un valor absurdo cae al valor por defecto en vez de romper.
        assertThat(historico(token, 0).get("meses").size()).isEqualTo(6);
        assertThat(historico(token, -4).get("meses").size()).isEqualTo(6);
    }

    /** RN-005: el historico de uno no incluye los movimientos de otro. */
    @Test
    @DisplayName("RN-005: el historico solo cuenta los movimientos del dueno del token")
    void elHistoricoNoSeMezcla() throws Exception {
        String ana = usuarioListo("ana.hist@finmind.test");
        String beto = usuarioListo("beto.hist@finmind.test");
        long cuenta = crearCuenta(ana);
        gastarEnFecha(ana, cuenta, categoria(ana, "GASTO"), "700000.00", MES_PASADO.atDay(6));

        assertThat(mesDe(historico(ana, null).get("meses"), MES_PASADO)
                .get("gastos").asDouble()).isEqualTo(700000.00);
        assertThat(mesDe(historico(beto, null).get("meses"), MES_PASADO)
                .get("gastos").asDouble()).isZero();
    }

    @Test
    @DisplayName("El historico exige sesion")
    void exigeToken() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/historico")).andExpect(status().isUnauthorized());
    }

    // ============================================ RN-032: la variacion

    /**
     * La cifra que la investigacion de UX senala como la de mayor efecto:
     * "vas gastando X mas que a esta altura del mes pasado".
     *
     * SOBRE LAS FECHAS ELEGIDAS
     * El gasto de este mes va HOY y el del mes anterior va el DIA 1. Asi la
     * prueba pasa cualquier dia del mes: el dia 1 del mes anterior siempre cae
     * dentro del tramo comparado, porque el corte nunca es menor que 1.
     * Ponerlos en un dia fijo como el 10 haria que la prueba fallara del 1 al 9.
     */
    @Test
    @DisplayName("La comparacion mide los mismos dias en los dos meses")
    void laComparacionMideElMismoTramo() throws Exception {
        String token = usuarioListo("hist@finmind.test");
        long cuenta = crearCuenta(token);
        long gasto = categoria(token, "GASTO");

        gastarEnFecha(token, cuenta, gasto, "100000.00", MES_PASADO.atDay(1));
        gastarEnFecha(token, cuenta, gasto, "150000.00", LocalDate.now());

        JsonNode c = historico(token, null).get("comparacion");
        assertThat(c.get("diaDeCorte").asInt()).isEqualTo(LocalDate.now().getDayOfMonth());
        assertThat(c.get("gastoEsteMes").asDouble()).isEqualTo(150000.00);
        assertThat(c.get("gastoMesAnterior").asDouble()).isEqualTo(100000.00);
        assertThat(c.get("variacion").asDouble()).isEqualTo(50000.00);
        assertThat(c.get("variacionPorcentaje").asDouble()).isEqualTo(50.0);
    }

    /**
     * La razon de ser del tramo recortado: un gasto del mes pasado posterior al
     * dia de hoy NO debe contar, porque este mes ese dia todavia no llega. Si
     * contara, la comparacion diria que el usuario va gastando menos solo
     * porque el mes va por la mitad.
     *
     * Solo se puede comprobar cuando hoy no es el ultimo dia del mes anterior:
     * si no, no queda ningun dia posterior al corte donde poner el gasto.
     */
    @Test
    @DisplayName("Lo que el mes pasado se gasto despues del dia de corte no cuenta")
    void elTramoPrevioSeRecorta() throws Exception {
        LocalDate hoy = LocalDate.now();
        int corte = Math.min(hoy.getDayOfMonth(), MES_PASADO.lengthOfMonth());
        // Sin dias despues del corte no hay nada que recortar; la regla ya la
        // cubre la prueba anterior.
        if (corte >= MES_PASADO.lengthOfMonth()) return;

        String token = usuarioListo("hist@finmind.test");
        long cuenta = crearCuenta(token);
        long gasto = categoria(token, "GASTO");

        gastarEnFecha(token, cuenta, gasto, "40000.00", MES_PASADO.atDay(1));
        // Este cae despues del corte: queda fuera de la comparacion...
        gastarEnFecha(token, cuenta, gasto, "999999.00",
                MES_PASADO.atDay(MES_PASADO.lengthOfMonth()));

        JsonNode r = historico(token, null);
        assertThat(r.get("comparacion").get("gastoMesAnterior").asDouble()).isEqualTo(40000.00);

        // ...pero SI cuenta en el total de ese mes en la serie: el mes pasado
        // se gasto todo eso, y la serie cuenta meses completos.
        assertThat(mesDe(r.get("meses"), MES_PASADO).get("gastos").asDouble())
                .isEqualTo(40000.00 + 999999.00);
    }

    /**
     * RN-032. Sin gasto en el tramo previo no hay porcentaje posible: una cifra
     * dividida por cero no es "infinito por ciento", es una pregunta mal
     * planteada. Se omite en vez de inventar un numero.
     */
    @Test
    @DisplayName("RN-032: sin gasto el mes anterior, el porcentaje se omite")
    void sinBaseNoHayPorcentaje() throws Exception {
        String token = usuarioListo("hist@finmind.test");
        long cuenta = crearCuenta(token);

        // Solo este mes tiene gasto; el tramo del mes anterior queda en cero.
        gastarEnFecha(token, cuenta, categoria(token, "GASTO"),
                "300000.00", LocalDate.now());

        JsonNode c = historico(token, null).get("comparacion");
        // La cifra absoluta si se puede dar: subio 300.000.
        assertThat(c.get("variacion").asDouble()).isEqualTo(300000.00);
        // El porcentaje no.
        assertThat(vino(c, "variacionPorcentaje")).isFalse();
    }

    @Test
    @DisplayName("Un usuario sin ningun movimiento recibe la comparacion en ceros")
    void sinMovimientosLaComparacionEsCero() throws Exception {
        String token = usuarioListo("vacio.hist@finmind.test");

        JsonNode c = historico(token, null).get("comparacion");
        assertThat(c.get("gastoEsteMes").asDouble()).isZero();
        assertThat(c.get("gastoMesAnterior").asDouble()).isZero();
        assertThat(c.get("variacion").asDouble()).isZero();
        // Cero contra cero tampoco tiene porcentaje.
        assertThat(vino(c, "variacionPorcentaje")).isFalse();
    }

    // ================================ RF-050: comparacion por categoria

    /**
     * La comparacion por categoria es la que permite decir "gastaste 80.000 mas
     * en Alimentacion". Sin ella el usuario ve que gasto mas, pero no en que, y
     * no hay nada concreto que pueda cambiar.
     */
    @Test
    @DisplayName("La composicion del gasto trae el monto del mes anterior y la variacion")
    void laComposicionSeCompara() throws Exception {
        String token = usuarioListo("hist@finmind.test");
        long cuenta = crearCuenta(token);
        long gasto = categoria(token, "GASTO");

        gastarEnFecha(token, cuenta, gasto, "100000.00", HACE_DOS_MESES.atDay(10));
        gastarEnFecha(token, cuenta, gasto, "180000.00", MES_PASADO.atDay(10));

        JsonNode porcion = composicion(token, MES_PASADO).get("porciones").get(0);
        assertThat(porcion.get("monto").asDouble()).isEqualTo(180000.00);
        assertThat(porcion.get("montoMesAnterior").asDouble()).isEqualTo(100000.00);
        assertThat(porcion.get("variacion").asDouble()).isEqualTo(80000.00);
    }

    /**
     * Una categoria que existia y no se uso el mes pasado si es comparable: el
     * gasto anterior fue cero de verdad. Distinto es no tener con que comparar.
     */
    @Test
    @DisplayName("Una categoria nueva este mes se compara contra cero")
    void categoriaSinGastoPrevioSeComparaContraCero() throws Exception {
        String token = usuarioListo("hist@finmind.test");
        long cuenta = crearCuenta(token);

        gastarEnFecha(token, cuenta, categoria(token, "GASTO"),
                "90000.00", MES_PASADO.atDay(10));

        JsonNode porcion = composicion(token, MES_PASADO).get("porciones").get(0);
        assertThat(porcion.get("montoMesAnterior").asDouble()).isZero();
        assertThat(porcion.get("variacion").asDouble()).isEqualTo(90000.00);
    }

    // ------------------------------------------------------------------ apoyo

    /**
     * Si un campo vino en el JSON.
     *
     * La aplicacion usa default-property-inclusion: non_null, asi que un campo
     * nulo NO viaja como null: no viaja. `get()` devuelve null de Java y no un
     * NullNode, con lo que llamar .isNull() encima lanza NullPointerException.
     * Se comprueban las dos formas para que la prueba siga siendo valida si esa
     * configuracion cambiara.
     */
    private boolean vino(JsonNode nodo, String campo) {
        JsonNode c = nodo.get(campo);
        return c != null && !c.isNull();
    }

    /** Busca un mes por su anio y mes, no por su posicion en la lista. */
    private JsonNode mesDe(JsonNode meses, YearMonth buscado) {
        for (JsonNode m : meses) {
            if (m.get("anio").asInt() == buscado.getYear()
                    && m.get("mes").asInt() == buscado.getMonthValue()) {
                return m;
            }
        }
        throw new IllegalStateException("No vino " + buscado + " en la serie");
    }

    private JsonNode historico(String token, Integer meses) throws Exception {
        String ruta = "/api/v1/reportes/historico" + (meses == null ? "" : "?meses=" + meses);
        String r = mockMvc.perform(get(ruta).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r);
    }

    private JsonNode composicion(String token, YearMonth mes) throws Exception {
        String r = mockMvc.perform(get("/api/v1/reportes/gasto-por-categoria?anio=%d&mes=%d"
                        .formatted(mes.getYear(), mes.getMonthValue()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r);
    }

    private String usuarioListo(String correo) throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Prueba","apellido":"Usuario","correo":"%s","contrasena":"%s"}"""
                                .formatted(correo, CONTRASENA)))
                .andExpect(status().isCreated());
        Usuario creado = usuarioRepository.findByCorreo(correo).orElseThrow();
        String codigo = codigoRepository
                .findByUsuarioIdAndTipoAndUsadoEnIsNull(creado.getId(), CodigoVerificacion.VERIFICACION)
                .orElseThrow().getCodigo();
        String r = mockMvc.perform(post("/api/v1/auth/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","codigo":"%s"}""".formatted(correo, codigo)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r).get("token").asText();
    }

    private long crearCuenta(String token) throws Exception {
        String r = mockMvc.perform(post("/api/v1/cuentas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Ahorros","tipo":"AHORROS","saldoInicial":9000000.00}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r).get("id").asLong();
    }

    private long categoria(String token, String tipo) throws Exception {
        String r = mockMvc.perform(get("/api/v1/categorias?tipo=" + tipo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r).get(0).get("id").asLong();
    }

    private void gastarEnFecha(String token, long cuenta, long categoria, String monto,
                               LocalDate fecha) throws Exception {
        registrarEnFecha(token, cuenta, categoria, monto, fecha);
    }

    /** El tipo del movimiento lo define la categoria (RN-002). */
    private void registrarEnFecha(String token, long cuenta, long categoria, String monto,
                                  LocalDate fecha) throws Exception {
        mockMvc.perform(post("/api/v1/transacciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"cuentaId":%d,"categoriaId":%d,"monto":%s,"fecha":"%s"}"""
                                .formatted(cuenta, categoria, monto, fecha)))
                .andExpect(status().isCreated());
    }
}
