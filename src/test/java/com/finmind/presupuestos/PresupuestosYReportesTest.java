package com.finmind.presupuestos;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Presupuestos y reportes (RF-017 a RF-022).
 *
 * Todo se prueba contra el mes en curso porque es el periodo por defecto de la
 * aplicacion y el que vera el usuario al entrar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Presupuestos y reportes")
class PresupuestosYReportesTest {

    private static final String CONTRASENA = "ClaveSegura123";
    private static final YearMonth AHORA = YearMonth.now();
    private static final short ANIO = (short) AHORA.getYear();
    private static final short MES = (short) AHORA.getMonthValue();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CodigoVerificacionRepository codigoRepository;
    @Autowired private LimpiadorDeDatos limpiador;

    @BeforeEach
    void prepararDatos() {
        limpiador.limpiar();
    }

    // ------------------------------------------------ RF-017 y RN-006

    @Test
    @DisplayName("Se define un presupuesto y nace sin consumo")
    void seDefineUnPresupuesto() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cat = categoria(token, "GASTO");

        mockMvc.perform(post("/api/v1/presupuestos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"categoriaId":%d,"montoLimite":500000.00,"anio":%d,"mes":%d}"""
                                .formatted(cat, ANIO, MES)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montoLimite").value(500000.00))
                .andExpect(jsonPath("$.consumo").value(0))
                .andExpect(jsonPath("$.disponible").value(500000.00))
                .andExpect(jsonPath("$.porcentajeConsumido").value(0.0))
                .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.periodo").value("MENSUAL"));
    }

    @Test
    @DisplayName("RN-006: no hay dos presupuestos de la misma categoria en el mismo mes")
    void noSeRepitePorPeriodo() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cat = categoria(token, "GASTO");
        crearPresupuesto(token, cat, "500000.00");

        mockMvc.perform(post("/api/v1/presupuestos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"categoriaId":%d,"montoLimite":900000.00,"anio":%d,"mes":%d}"""
                                .formatted(cat, ANIO, MES)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Una categoria de ingreso no se presupuesta")
    void soloSePresupuestaElGasto() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long catIngreso = categoria(token, "INGRESO");

        mockMvc.perform(post("/api/v1/presupuestos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"categoriaId":%d,"montoLimite":500000.00,"anio":%d,"mes":%d}"""
                                .formatted(catIngreso, ANIO, MES)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un mes fuera de rango se rechaza")
    void mesInvalido() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cat = categoria(token, "GASTO");
        mockMvc.perform(post("/api/v1/presupuestos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"categoriaId":%d,"montoLimite":500000.00,"anio":%d,"mes":13}"""
                                .formatted(cat, ANIO)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------ RN-009 y RF-019

    @Test
    @DisplayName("RN-009: el consumo sale de los movimientos, no de un campo guardado")
    void elConsumoSeCalcula() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cuenta = crearCuenta(token);
        long cat = categoria(token, "GASTO");
        crearPresupuesto(token, cat, "500000.00");

        gastar(token, cuenta, cat, "300000.00");

        // 300.000 de 500.000 = 60%, quedan 200.000
        mockMvc.perform(get("/api/v1/presupuestos?anio=" + ANIO + "&mes=" + MES)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].consumo").value(300000.00))
                .andExpect(jsonPath("$[0].disponible").value(200000.00))
                .andExpect(jsonPath("$[0].porcentajeConsumido").value(60.0))
                .andExpect(jsonPath("$[0].estado").value("EN_CURSO"))
                .andExpect(jsonPath("$[0].aviso").doesNotExist());
    }

    @Test
    @DisplayName("RF-019: al llegar al 80% avisa, y al pasarse dice cuanto")
    void avisaAntesYDespuesDePasarse() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cuenta = crearCuenta(token);
        long cat = categoria(token, "GASTO");
        long presupuesto = crearPresupuesto(token, cat, "500000.00");

        gastar(token, cuenta, cat, "400000.00");
        mockMvc.perform(get("/api/v1/presupuestos/" + presupuesto)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.porcentajeConsumido").value(80.0))
                .andExpect(jsonPath("$.estado").value("EN_ALERTA"))
                // El aviso va en texto: el color no puede ser el unico indicador.
                .andExpect(jsonPath("$.aviso").exists());

        gastar(token, cuenta, cat, "200000.00");
        mockMvc.perform(get("/api/v1/presupuestos/" + presupuesto)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.consumo").value(600000.00))
                .andExpect(jsonPath("$.disponible").value(-100000.00))
                .andExpect(jsonPath("$.porcentajeConsumido").value(120.0))
                .andExpect(jsonPath("$.estado").value("EXCEDIDO"))
                .andExpect(jsonPath("$.aviso").exists());
    }

    @Test
    @DisplayName("Un ingreso de la misma categoria no libera presupuesto")
    void elIngresoNoLiberaPresupuesto() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cuenta = crearCuenta(token);
        long catGasto = categoria(token, "GASTO");
        long catIngreso = categoria(token, "INGRESO");
        crearPresupuesto(token, catGasto, "500000.00");

        gastar(token, cuenta, catGasto, "300000.00");
        gastar(token, cuenta, catIngreso, "1000000.00");   // es INGRESO por su categoria

        mockMvc.perform(get("/api/v1/presupuestos").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].consumo").value(300000.00));
    }

    @Test
    @DisplayName("Un gasto de otro mes no cuenta en este presupuesto")
    void otroMesNoCuenta() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cuenta = crearCuenta(token);
        long cat = categoria(token, "GASTO");
        crearPresupuesto(token, cat, "500000.00");

        // Un gasto del mes pasado, con fecha valida (no futura).
        LocalDate mesPasado = AHORA.minusMonths(1).atDay(15);
        mockMvc.perform(post("/api/v1/transacciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"cuentaId":%d,"categoriaId":%d,"monto":400000.00,"fecha":"%s"}"""
                                .formatted(cuenta, cat, mesPasado)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/presupuestos").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].consumo").value(0));
    }

    // ------------------------------------------------ RF-021

    @Test
    @DisplayName("RF-021: el balance del periodo")
    void balanceDelPeriodo() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cuenta = crearCuenta(token);
        long ingreso = categoria(token, "INGRESO");
        long gasto = categoria(token, "GASTO");

        gastar(token, cuenta, ingreso, "2000000.00");
        gastar(token, cuenta, gasto, "1200000.00");

        mockMvc.perform(get("/api/v1/reportes/balance").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingresos").value(2000000.00))
                .andExpect(jsonPath("$.gastos").value(1200000.00))
                .andExpect(jsonPath("$.diferencia").value(800000.00))
                .andExpect(jsonPath("$.porcentajeGastado").value(60.0))
                .andExpect(jsonPath("$.lectura").exists());
    }

    @Test
    @DisplayName("Gastar mas de lo que entra se explica con palabras, no solo con el signo")
    void balanceNegativoSeExplica() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cuenta = crearCuenta(token);
        long ingreso = categoria(token, "INGRESO");
        long gasto = categoria(token, "GASTO");

        gastar(token, cuenta, ingreso, "1000000.00");
        gastar(token, cuenta, gasto, "1500000.00");

        mockMvc.perform(get("/api/v1/reportes/balance").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.diferencia").value(-500000.00))
                .andExpect(jsonPath("$.porcentajeGastado").value(150.0))
                .andExpect(jsonPath("$.lectura").value("Gastaste 500000.00 mas de lo que ingresaste."));
    }

    @Test
    @DisplayName("Sin movimientos el balance no revienta")
    void balanceVacio() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        mockMvc.perform(get("/api/v1/reportes/balance").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingresos").value(0))
                .andExpect(jsonPath("$.porcentajeGastado").value(0.0))
                .andExpect(jsonPath("$.lectura").value("No hay movimientos en este periodo."));
    }

    // ------------------------------------------------ RF-022

    @Test
    @DisplayName("RF-022: la composicion del gasto viene ordenada de mayor a menor")
    void composicionOrdenada() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cuenta = crearCuenta(token);
        String r = mockMvc.perform(get("/api/v1/categorias?tipo=GASTO")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        long cat1 = objectMapper.readTree(r).get(0).get("id").asLong();
        long cat2 = objectMapper.readTree(r).get(1).get("id").asLong();

        gastar(token, cuenta, cat1, "100000.00");
        gastar(token, cuenta, cat2, "300000.00");

        mockMvc.perform(get("/api/v1/reportes/gasto-por-categoria")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(400000.00))
                .andExpect(jsonPath("$.porciones.length()").value(2))
                // La mayor va primero: 300.000 de 400.000 es el 75%
                .andExpect(jsonPath("$.porciones[0].monto").value(300000.00))
                .andExpect(jsonPath("$.porciones[0].porcentaje").value(75.0))
                .andExpect(jsonPath("$.porciones[1].porcentaje").value(25.0));
    }

    @Test
    @DisplayName("Sin gastos la composicion queda vacia y con total cero")
    void composicionVacia() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        mockMvc.perform(get("/api/v1/reportes/gasto-por-categoria")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.porciones.length()").value(0));
    }

    // ------------------------------------------------ Panel

    @Test
    @DisplayName("El panel trae balance, composicion, patrimonio y solo las alertas")
    void panelCompleto() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cuenta = crearCuenta(token);
        long gasto = categoria(token, "GASTO");
        long tranquilo = otraCategoriaDeGasto(token, gasto);

        crearPresupuesto(token, gasto, "100000.00");      // se va a exceder
        crearPresupuesto(token, tranquilo, "900000.00");  // se queda EN_CURSO

        gastar(token, cuenta, gasto, "150000.00");
        gastar(token, cuenta, tranquilo, "50000.00");

        mockMvc.perform(get("/api/v1/reportes/panel").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance.gastos").value(200000.00))
                .andExpect(jsonPath("$.gastoPorCategoria.total").value(200000.00))
                .andExpect(jsonPath("$.patrimonio.patrimonioNeto").exists())
                // Solo el excedido: mostrar los que van bien haria que nadie los mire.
                .andExpect(jsonPath("$.presupuestosEnAlerta.length()").value(1))
                .andExpect(jsonPath("$.presupuestosEnAlerta[0].estado").value("EXCEDIDO"));
    }

    @Test
    @DisplayName("RF-040: el resumen rapido trae hoy, la semana y el mes")
    void resumenRapido() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cuenta = crearCuenta(token);
        long ingreso = categoria(token, "INGRESO");
        long gasto = categoria(token, "GASTO");

        gastar(token, cuenta, ingreso, "80000.00");
        gastar(token, cuenta, gasto, "30000.00");

        // Todo se registro hoy, asi que los tres periodos coinciden.
        mockMvc.perform(get("/api/v1/reportes/resumen-rapido")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoy.ingresos").value(80000.00))
                .andExpect(jsonPath("$.hoy.gastos").value(30000.00))
                .andExpect(jsonPath("$.hoy.neto").value(50000.00))
                .andExpect(jsonPath("$.semana.neto").value(50000.00))
                .andExpect(jsonPath("$.mes.neto").value(50000.00));
    }

    // ------------------------------------------------ RN-005

    @Test
    @DisplayName("RN-005: el presupuesto de otro usuario responde 404")
    void aislamientoEntreUsuarios() throws Exception {
        String tokenAna = usuarioListo("ana@finmind.test");
        long cat = categoria(tokenAna, "GASTO");
        long deAna = crearPresupuesto(tokenAna, cat, "500000.00");

        String tokenLuis = usuarioListo("luis@finmind.test");
        mockMvc.perform(get("/api/v1/presupuestos/" + deAna)
                .header("Authorization", "Bearer " + tokenLuis)).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/presupuestos/" + deAna)
                        .header("Authorization", "Bearer " + tokenLuis)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montoLimite":10.00}"""))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/presupuestos").header("Authorization", "Bearer " + tokenLuis))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Sin token no se llega a presupuestos ni a reportes")
    void sinTokenNada() throws Exception {
        mockMvc.perform(get("/api/v1/presupuestos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/reportes/balance")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/reportes/panel")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/reportes/resumen-rapido")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------ apoyo

    // ------------------------------------ RN-024: el periodo se respeta (DEF-17)

    @Test
    @DisplayName("DEF-17: un presupuesto semanal se mide contra la semana, no contra el mes")
    void elPresupuestoSemanalNoMideElMesEntero() throws Exception {
        String token = usuarioListo("periodo@finmind.test");
        long cuenta = crearCuenta(token);
        long cat = categoria(token, "GASTO");

        // Un gasto de hoy: cae dentro de la semana en curso.
        gastar(token, cuenta, cat, "40000.00");
        // Y uno de hace 20 dias: mismo mes, pero otra semana. Puede caer en el
        // mes anterior si hoy es dia 1 a 20, y entonces no cuenta igual: por eso
        // la comprobacion de abajo mira el rango que devuelve el servidor.
        gastarEnFecha(token, cuenta, cat, "500000.00", LocalDate.now().minusDays(20));

        String r = mockMvc.perform(post("/api/v1/presupuestos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"categoriaId":%d,"montoLimite":100000.00,"periodo":"SEMANAL",
                                 "anio":%d,"mes":%d}""".formatted(cat, ANIO, MES)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.periodo").value("SEMANAL"))
                // Antes, este presupuesto de 100.000 se comparaba contra los
                // 540.000 del mes y decia 540%. Ahora solo cuenta la semana.
                .andExpect(jsonPath("$.consumo").value(40000.00))
                .andExpect(jsonPath("$.porcentajeConsumido").value(40.0))
                .andReturn().getResponse().getContentAsString();

        // Y la respuesta dice contra que fechas midio, que es lo que permite al
        // usuario entender el porcentaje.
        var nodo = objectMapper.readTree(r);
        LocalDate desde = LocalDate.parse(nodo.get("desde").asText());
        LocalDate hasta = LocalDate.parse(nodo.get("hasta").asText());
        org.assertj.core.api.Assertions.assertThat(desde).isBeforeOrEqualTo(LocalDate.now());
        org.assertj.core.api.Assertions.assertThat(hasta).isAfterOrEqualTo(LocalDate.now());
        org.assertj.core.api.Assertions.assertThat(
                java.time.temporal.ChronoUnit.DAYS.between(desde, hasta)).isLessThan(7);
    }

    @Test
    @DisplayName("RN-024: un presupuesto mensual sigue midiendo el mes completo")
    void elPresupuestoMensualNoCambia() throws Exception {
        String token = usuarioListo("periodo@finmind.test");
        long cuenta = crearCuenta(token);
        long cat = categoria(token, "GASTO");

        gastar(token, cuenta, cat, "40000.00");
        gastarEnFecha(token, cuenta, cat, "60000.00", primerDiaDelMes());

        // El arreglo del periodo no debe haber tocado el comportamiento anterior.
        mockMvc.perform(post("/api/v1/presupuestos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"categoriaId":%d,"montoLimite":500000.00,"periodo":"MENSUAL",
                                 "anio":%d,"mes":%d}""".formatted(cat, ANIO, MES)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consumo").value(100000.00))
                .andExpect(jsonPath("$.desde").value(primerDiaDelMes().toString()));
    }

    @Test
    @DisplayName("RN-024: sin periodo explicito se asume mensual")
    void sinPeriodoSeAsumeMensual() throws Exception {
        String token = usuarioListo("periodo@finmind.test");
        long cat = categoria(token, "GASTO");

        mockMvc.perform(post("/api/v1/presupuestos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"categoriaId":%d,"montoLimite":300000.00,"anio":%d,"mes":%d}"""
                                .formatted(cat, ANIO, MES)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.periodo").value("MENSUAL"));
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
        String r = mockMvc.perform(post("/api/v1/auth/verificar").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","codigo":"%s"}""".formatted(correo, codigo)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r).get("token").asText();
    }

    private long crearCuenta(String token) throws Exception {
        String r = mockMvc.perform(post("/api/v1/cuentas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ahorros","tipo":"AHORROS","saldoInicial":5000000.00}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r).get("id").asLong();
    }

    private long categoria(String token, String tipo) throws Exception {
        String r = mockMvc.perform(get("/api/v1/categorias?tipo=" + tipo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r).get(0).get("id").asLong();
    }

    private long otraCategoriaDeGasto(String token, long distintaDe) throws Exception {
        String r = mockMvc.perform(get("/api/v1/categorias?tipo=GASTO")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        var nodo = objectMapper.readTree(r);
        for (int i = 0; i < nodo.size(); i++) {
            long id = nodo.get(i).get("id").asLong();
            if (id != distintaDe) return id;
        }
        throw new IllegalStateException("Se esperaba mas de una categoria de gasto");
    }

    private long crearPresupuesto(String token, long categoriaId, String limite) throws Exception {
        String r = mockMvc.perform(post("/api/v1/presupuestos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"categoriaId":%d,"montoLimite":%s,"anio":%d,"mes":%d}"""
                                .formatted(categoriaId, limite, ANIO, MES)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r).get("id").asLong();
    }

    /** Registra un movimiento; su tipo lo define la categoria (RN-002). */
    private void gastar(String token, long cuenta, long categoria, String monto) throws Exception {
        mockMvc.perform(post("/api/v1/transacciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"cuentaId":%d,"categoriaId":%d,"monto":%s,"fecha":"%s"}"""
                                .formatted(cuenta, categoria, monto, LocalDate.now())))
                .andExpect(status().isCreated());
    }

    /** Registra un gasto en una fecha concreta, para probar ventanas de tiempo. */
    private void gastarEnFecha(String token, long cuenta, long categoria, String monto,
                               LocalDate fecha) throws Exception {
        mockMvc.perform(post("/api/v1/transacciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"cuentaId":%d,"categoriaId":%d,"monto":%s,"fecha":"%s"}"""
                                .formatted(cuenta, categoria, monto, fecha)))
                .andExpect(status().isCreated());
    }

    private LocalDate primerDiaDelMes() {
        return LocalDate.now().withDayOfMonth(1);
    }
}
