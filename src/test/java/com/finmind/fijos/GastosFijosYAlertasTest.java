package com.finmind.fijos;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gastos fijos y alertas (RF-046, RF-047, RN-025 a RN-031).
 *
 * POR QUE ESTAS DOS COSAS SE PRUEBAN JUNTAS
 * Una alerta no tiene con que compararse si no existen los compromisos. Probar
 * las alertas aparte obligaria a duplicar aqui el montaje de gastos fijos, y una
 * copia se desactualiza.
 *
 * SOBRE EL DIA DEL MES
 * Estas pruebas corren cualquier dia. El servicio no proyecta antes del dia 5
 * (con dos dias de datos la proyeccion miente), asi que las pruebas que dependen
 * de la proyeccion NO afirman "la alerta esta": afirman la regla completa —
 * aparece si y solo si ya pasaron los dias minimos — leyendo del propio cuerpo
 * de la respuesta cuantos dias van. Una prueba que solo pasara del 5 en adelante
 * seria una prueba que falla sola una vez al mes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Gastos fijos y alertas")
class GastosFijosYAlertasTest {

    private static final String CONTRASENA = "ClaveSegura123";
    private static final YearMonth AHORA = YearMonth.now();

    /** Mismo umbral que ServicioAlertas.DIAS_MINIMOS_PARA_PROYECTAR. */
    private static final int DIAS_MINIMOS = 5;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CodigoVerificacionRepository codigoRepository;
    @Autowired private LimpiadorDeDatos limpiador;

    @BeforeEach
    void prepararDatos() {
        limpiador.limpiar();
    }

    // =================================================== RF-046: gastos fijos

    @Test
    @DisplayName("Se registra un compromiso mensual y nace activo")
    void seRegistraUnGastoFijo() throws Exception {
        String token = usuarioListo("fijo@finmind.test");
        long cat = categoria(token, "GASTO");

        mockMvc.perform(post("/api/v1/gastos-fijos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Arriendo","categoriaId":%d,"monto":1100000.00,
                                 "periodicidad":"MENSUAL","diaPago":2}""".formatted(cat)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Arriendo"))
                .andExpect(jsonPath("$.monto").value(1100000.00))
                .andExpect(jsonPath("$.montoMensual").value(1100000.00))
                .andExpect(jsonPath("$.periodicidad").value("MENSUAL"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.cubiertoEsteMes").value(false))
                .andExpect(jsonPath("$.proximoPago").isNotEmpty());
    }

    /**
     * RN-025. La conversion es lo que evita subestimar el gasto por cuatro: un
     * compromiso semanal de 50.000 pesa 217.250 al mes (50.000 x 4,345).
     */
    @Test
    @DisplayName("RN-025: un compromiso semanal se convierte a su equivalente mensual")
    void elSemanalSeLlevaAMensual() throws Exception {
        String token = usuarioListo("fijo@finmind.test");
        long cat = categoria(token, "GASTO");

        mockMvc.perform(post("/api/v1/gastos-fijos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Mercado semanal","categoriaId":%d,"monto":50000.00,
                                 "periodicidad":"SEMANAL","diaPago":1}""".formatted(cat)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monto").value(50000.00))
                .andExpect(jsonPath("$.montoMensual").value(217250.00));
    }

    @Test
    @DisplayName("RN-025: un compromiso quincenal pesa el doble en el mes")
    void elQuincenalPesaElDoble() throws Exception {
        String token = usuarioListo("fijo@finmind.test");
        long cat = categoria(token, "GASTO");

        mockMvc.perform(post("/api/v1/gastos-fijos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Transporte","categoriaId":%d,"monto":120000.00,
                                 "periodicidad":"QUINCENAL","diaPago":15}""".formatted(cat)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montoMensual").value(240000.00));
    }

    @Test
    @DisplayName("Un compromiso no puede apuntar a una categoria de ingreso")
    void soloCategoriasDeGasto() throws Exception {
        String token = usuarioListo("fijo@finmind.test");
        long catIngreso = categoria(token, "INGRESO");

        mockMvc.perform(post("/api/v1/gastos-fijos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Sueldo","categoriaId":%d,"monto":100000.00,
                                 "periodicidad":"MENSUAL","diaPago":1}""".formatted(catIngreso)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("No se repite el nombre de un compromiso")
    void noSeRepiteElNombre() throws Exception {
        String token = usuarioListo("fijo@finmind.test");
        long cat = categoria(token, "GASTO");
        crearFijo(token, cat, "Arriendo", "1100000.00", "MENSUAL", 2);

        mockMvc.perform(post("/api/v1/gastos-fijos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"arriendo","categoriaId":%d,"monto":900000.00,
                                 "periodicidad":"MENSUAL","diaPago":5}""".formatted(cat)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Una periodicidad inventada se rechaza")
    void periodicidadInvalida() throws Exception {
        String token = usuarioListo("fijo@finmind.test");
        long cat = categoria(token, "GASTO");

        mockMvc.perform(post("/api/v1/gastos-fijos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Netflix","categoriaId":%d,"monto":40000.00,
                                 "periodicidad":"TRIMESTRAL","diaPago":10}""".formatted(cat)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Se desactiva, no se borra: el compromiso de meses pasados sigue
     * explicando las alertas de esos meses.
     */
    @Test
    @DisplayName("Un compromiso se desactiva sin perderse")
    void seDesactivaSinBorrarse() throws Exception {
        String token = usuarioListo("fijo@finmind.test");
        long cat = categoria(token, "GASTO");
        long id = crearFijo(token, cat, "Gimnasio", "90000.00", "MENSUAL", 5);

        mockMvc.perform(patch("/api/v1/gastos-fijos/" + id + "/desactivar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        // Ya no aparece en la lista normal...
        mockMvc.perform(get("/api/v1/gastos-fijos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // ...pero sigue existiendo.
        mockMvc.perform(get("/api/v1/gastos-fijos?incluirInactivos=true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Gimnasio"));
    }

    /** RN-005: nadie ve ni toca los compromisos de otro. */
    @Test
    @DisplayName("RN-005: los compromisos de otro usuario no se ven ni se editan")
    void cadaUsuarioVeSoloLoSuyo() throws Exception {
        String ana = usuarioListo("ana.fijo@finmind.test");
        String beto = usuarioListo("beto.fijo@finmind.test");
        long id = crearFijo(ana, categoria(ana, "GASTO"), "Arriendo", "1100000.00", "MENSUAL", 2);

        mockMvc.perform(get("/api/v1/gastos-fijos").header("Authorization", "Bearer " + beto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(patch("/api/v1/gastos-fijos/" + id + "/desactivar")
                        .header("Authorization", "Bearer " + beto))
                .andExpect(status().isNotFound());
    }

    /**
     * RN-026. Si el gasto real de la categoria ya cubre el compromiso, deja de
     * contarse como pendiente. Sin esto la alerta diria que al usuario le falta
     * dinero para algo que ya pago.
     */
    @Test
    @DisplayName("RN-026: un compromiso ya pagado deja de estar pendiente")
    void elCompromisoPagadoNoCuenta() throws Exception {
        String token = usuarioListo("fijo@finmind.test");
        long cuenta = crearCuenta(token);
        long vivienda = categoria(token, "GASTO");
        crearFijo(token, vivienda, "Arriendo", "1100000.00", "MENSUAL", 2);

        // Antes de pagar: pendiente.
        assertThat(alertas(token).get("compromisosPendientes").asDouble()).isEqualTo(1100000.00);

        gastar(token, cuenta, vivienda, "1100000.00");

        // Despues de pagar: ya no.
        mockMvc.perform(get("/api/v1/gastos-fijos").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].cubiertoEsteMes").value(true));
        assertThat(alertas(token).get("compromisosPendientes").asDouble()).isZero();
    }

    // ======================================================= RF-047: alertas

    /**
     * RN-027. El caso que motivo el modulo entero: el usuario tiene 100.000 y
     * todavia debe 1.100.000 de arriendo.
     *
     * No depende del dia del mes: no hay proyeccion de por medio, solo resta.
     */
    @Test
    @DisplayName("RN-027: avisa que no alcanza para los compromisos y nombra cual")
    void avisaQueNoAlcanzaParaElArriendo() throws Exception {
        String token = usuarioListo("alerta@finmind.test");
        long cuenta = crearCuenta(token);
        long alimentacion = categoria(token, "GASTO");
        long vivienda = otraCategoriaDeGasto(token, alimentacion);
        long salario = categoria(token, "INGRESO");

        ingresar(token, cuenta, salario, "2000000.00");
        gastar(token, cuenta, alimentacion, "1900000.00");
        crearFijo(token, vivienda, "Arriendo", "1100000.00", "MENSUAL", 2);

        JsonNode r = alertas(token);

        // 2.000.000 - 1.900.000 = 100.000 disponibles; faltan 1.000.000.
        assertThat(r.get("ingresosDelMes").asDouble()).isEqualTo(2000000.00);
        assertThat(r.get("gastadoHastaHoy").asDouble()).isEqualTo(1900000.00);
        assertThat(r.get("disponible").asDouble()).isEqualTo(100000.00);
        assertThat(r.get("compromisosPendientes").asDouble()).isEqualTo(1100000.00);
        assertThat(r.get("holgura").asDouble()).isEqualTo(-1000000.00);

        JsonNode aviso = buscarAlerta(r, "FALTA_PARA_COMPROMISOS");
        assertThat(aviso).as("deberia existir la alerta de compromisos").isNotNull();
        assertThat(aviso.get("severidad").asText()).isEqualTo("ALTA");
        // Nombrar el compromiso es la diferencia entre un aviso util y uno vacio.
        assertThat(aviso.get("mensaje").asText()).contains("Arriendo");
        assertThat(aviso.get("monto").asDouble()).isEqualTo(1000000.00);
        assertThat(aviso.get("rutaSugerida").asText()).isEqualTo("/gastos-fijos");

        // Y la lectura en palabras dice lo mismo, sin depender del color (RNF-008).
        assertThat(r.get("lectura").asText()).contains("no cubres");
    }

    @Test
    @DisplayName("Si el dinero alcanza no se inventa una alarma")
    void sinProblemaNoHayAlarma() throws Exception {
        String token = usuarioListo("alerta@finmind.test");
        long cuenta = crearCuenta(token);
        long alimentacion = categoria(token, "GASTO");
        long vivienda = otraCategoriaDeGasto(token, alimentacion);
        long salario = categoria(token, "INGRESO");

        ingresar(token, cuenta, salario, "4000000.00");
        gastar(token, cuenta, alimentacion, "300000.00");
        crearFijo(token, vivienda, "Arriendo", "1100000.00", "MENSUAL", 2);

        JsonNode r = alertas(token);

        // 4.000.000 - 300.000 - 1.100.000 = 2.600.000 de holgura.
        assertThat(r.get("holgura").asDouble()).isEqualTo(2600000.00);
        assertThat(buscarAlerta(r, "FALTA_PARA_COMPROMISOS")).isNull();
        assertThat(buscarAlerta(r, "RITMO_ALTO")).isNull();
        assertThat(r.get("lectura").asText()).contains("Cubres tus compromisos");
    }

    /**
     * RN-028. Gasto muy por encima del ingreso: al ritmo actual el mes cierra
     * en rojo aunque hoy todavia quede dinero.
     *
     * La afirmacion es la regla completa, no solo el caso feliz: antes del dia 5
     * la alerta NO debe aparecer, porque con dos dias de datos la proyeccion
     * multiplica cualquier compra grande por quince.
     */
    @Test
    @DisplayName("RN-028: avisa del ritmo de gasto, y no antes del dia 5")
    void avisaDelRitmoSoloConDatosSuficientes() throws Exception {
        String token = usuarioListo("ritmo@finmind.test");
        long cuenta = crearCuenta(token);
        long gasto = categoria(token, "GASTO");
        long salario = categoria(token, "INGRESO");

        ingresar(token, cuenta, salario, "1000000.00");
        // El gasto ya supera al ingreso HOY. Se escoge asi a proposito: la
        // proyeccion nunca es menor que lo ya gastado, asi que el exceso queda
        // garantizado corra la prueba el dia 5 o el ultimo del mes. Con un gasto
        // menor al ingreso, la prueba pasaria a principio de mes y fallaria
        // sola el dia 30, cuando proyeccion y gasto real coinciden.
        gastar(token, cuenta, gasto, "1200000.00");

        JsonNode r = alertas(token);
        int dias = r.get("diasTranscurridos").asInt();

        // El ritmo y la proyeccion se publican siempre: son datos, no avisos.
        assertThat(r.get("ritmoDiario").asDouble()).isEqualTo(redondear(1200000.0 / dias));
        // Lo que importa es que la proyeccion supere al ingreso, que es la
        // condicion del aviso. No se compara contra 1.200.000 exactos porque el
        // ritmo se redondea a dos decimales antes de multiplicarse y el
        // resultado puede quedar unos centavos por debajo.
        assertThat(r.get("proyeccionFinDeMes").asDouble()).isGreaterThan(1000000.00);

        JsonNode aviso = buscarAlerta(r, "RITMO_ALTO");
        if (dias >= DIAS_MINIMOS) {
            assertThat(aviso).as("con %d dias ya hay datos para proyectar", dias).isNotNull();
            assertThat(aviso.get("severidad").asText()).isEqualTo("MEDIA");
            assertThat(aviso.get("rutaSugerida").asText()).isEqualTo("/movimientos");
        } else {
            assertThat(aviso).as("con solo %d dias la proyeccion mentiria", dias).isNull();
        }
    }

    @Test
    @DisplayName("RN-028: sin ingresos registrados no se acusa al usuario de gastar de mas")
    void sinIngresosNoHayAlertaDeRitmo() throws Exception {
        String token = usuarioListo("ritmo@finmind.test");
        long cuenta = crearCuenta(token);
        long gasto = categoria(token, "GASTO");

        // Gasta, pero nunca registro un ingreso: comparar contra cero diria
        // siempre que se esta excediendo, y eso no informa nada.
        gastar(token, cuenta, gasto, "900000.00");

        assertThat(buscarAlerta(alertas(token), "RITMO_ALTO")).isNull();
    }

    /**
     * RN-029. El aviso llega antes de que el presupuesto se rompa. El que ya
     * existia esperaba al 80% consumido; este mira hacia donde va.
     */
    @Test
    @DisplayName("RN-029: avisa de un presupuesto que se va a exceder antes de excederse")
    void avisaDelPresupuestoEnRiesgo() throws Exception {
        String token = usuarioListo("presupuesto@finmind.test");
        long cuenta = crearCuenta(token);
        long cat = categoria(token, "GASTO");

        // El limite deja el presupuesto en 83%: alto, pero no excedido, que es
        // justo el hueco que este aviso cubre.
        crearPresupuesto(token, cat, "3000000.00");
        gastar(token, cuenta, cat, "2500000.00");

        JsonNode r = alertas(token);
        int dias = r.get("diasTranscurridos").asInt();
        int diasDelMes = r.get("diasDelMes").asInt();

        // La regla se recalcula aqui en lugar de fijar un resultado: el ultimo
        // dia del mes la proyeccion es igual al gasto real (2.500.000) y no
        // supera el limite, asi que ese dia NO debe haber aviso. Afirmar que
        // siempre lo hay seria una prueba que se rompe sola una vez al mes.
        double proyeccion = redondear(2500000.0 / dias) * diasDelMes;
        boolean deberiaAvisar = dias >= DIAS_MINIMOS && proyeccion > 3000000.0;

        JsonNode aviso = buscarAlerta(r, "PRESUPUESTO_EN_RIESGO");
        if (deberiaAvisar) {
            assertThat(aviso).as("dia %d de %d, proyeccion %.2f", dias, diasDelMes, proyeccion)
                    .isNotNull();
            assertThat(aviso.get("severidad").asText()).isEqualTo("MEDIA");
            assertThat(aviso.get("rutaSugerida").asText()).isEqualTo("/presupuestos");
            assertThat(aviso.get("titulo").asText()).startsWith("Vas a pasarte en");
        } else {
            assertThat(aviso).as("dia %d de %d, proyeccion %.2f", dias, diasDelMes, proyeccion)
                    .isNull();
        }
    }

    /**
     * Un presupuesto ya excedido no genera este aviso: lo reporta el modulo de
     * presupuestos. Dos avisos para el mismo hecho es ruido.
     */
    @Test
    @DisplayName("RN-029: el presupuesto ya excedido no se avisa dos veces")
    void elExcedidoNoSeRepite() throws Exception {
        String token = usuarioListo("presupuesto@finmind.test");
        long cuenta = crearCuenta(token);
        long cat = categoria(token, "GASTO");

        crearPresupuesto(token, cat, "100000.00");
        gastar(token, cuenta, cat, "400000.00");

        assertThat(buscarAlerta(alertas(token), "PRESUPUESTO_EN_RIESGO")).isNull();
    }

    /** RN-031. Tarjeta con el cupo casi consumido. No depende del dia. */
    @Test
    @DisplayName("RN-031: avisa cuando la tarjeta va por el 85% del cupo")
    void avisaDelCupoCasiAgotado() throws Exception {
        String token = usuarioListo("cupo@finmind.test");
        crearTarjeta(token, "Visa", "900000.00", "1000000.00");

        JsonNode aviso = buscarAlerta(alertas(token), "CUPO_CASI_AGOTADO");
        assertThat(aviso).isNotNull();
        assertThat(aviso.get("severidad").asText()).isEqualTo("BAJA");
        assertThat(aviso.get("titulo").asText()).contains("Visa");
        assertThat(aviso.get("monto").asDouble()).isEqualTo(100000.00);
    }

    @Test
    @DisplayName("RN-031: pasarse del cupo es grave, no un aviso menor")
    void pasarseDelCupoEsAlta() throws Exception {
        String token = usuarioListo("cupo@finmind.test");
        crearTarjeta(token, "Visa", "1200000.00", "1000000.00");

        JsonNode aviso = buscarAlerta(alertas(token), "CUPO_CASI_AGOTADO");
        assertThat(aviso).isNotNull();
        assertThat(aviso.get("severidad").asText()).isEqualTo("ALTA");
        assertThat(aviso.get("mensaje").asText()).contains("1200000");
    }

    @Test
    @DisplayName("Una tarjeta con cupo de sobra no molesta")
    void tarjetaHolgadaNoAvisa() throws Exception {
        String token = usuarioListo("cupo@finmind.test");
        crearTarjeta(token, "Visa", "200000.00", "1000000.00");

        assertThat(buscarAlerta(alertas(token), "CUPO_CASI_AGOTADO")).isNull();
    }

    /**
     * Un usuario recien registrado no debe recibir una pared de avisos. El
     * primer contacto con la aplicacion decide si vuelve.
     */
    @Test
    @DisplayName("Un usuario sin datos no recibe ninguna alerta")
    void usuarioNuevoSinAlertas() throws Exception {
        String token = usuarioListo("nuevo@finmind.test");

        JsonNode r = alertas(token);
        assertThat(r.get("alertas")).isEmpty();
        assertThat(r.get("compromisosPendientes").asDouble()).isZero();
        assertThat(r.get("diasDelMes").asInt()).isEqualTo(AHORA.lengthOfMonth());
        assertThat(r.get("lectura").asText()).contains("No tienes compromisos");
    }

    @Test
    @DisplayName("Las alertas exigen sesion")
    void alertasExigenToken() throws Exception {
        mockMvc.perform(get("/api/v1/alertas")).andExpect(status().isUnauthorized());
    }

    /** RN-005: las alertas de uno no se calculan con los datos de otro. */
    @Test
    @DisplayName("RN-005: las alertas solo miran los datos del dueno del token")
    void lasAlertasNoSeMezclan() throws Exception {
        String ana = usuarioListo("ana.alerta@finmind.test");
        String beto = usuarioListo("beto.alerta@finmind.test");
        crearFijo(ana, categoria(ana, "GASTO"), "Arriendo", "1100000.00", "MENSUAL", 2);

        assertThat(alertas(beto).get("compromisosPendientes").asDouble()).isZero();
        assertThat(alertas(ana).get("compromisosPendientes").asDouble()).isEqualTo(1100000.00);
    }

    // ------------------------------------------------------------------ apoyo

    /** Devuelve la alerta del tipo pedido, o null si el servicio no la emitio. */
    private JsonNode buscarAlerta(JsonNode resumen, String tipo) {
        for (JsonNode a : resumen.get("alertas")) {
            if (tipo.equals(a.get("tipo").asText())) return a;
        }
        return null;
    }

    private JsonNode alertas(String token) throws Exception {
        String r = mockMvc.perform(get("/api/v1/alertas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r);
    }

    /** Mismo redondeo que el servicio: dos decimales, HALF_UP. */
    private double redondear(double v) {
        return new java.math.BigDecimal(Double.toString(v))
                .setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    private long crearFijo(String token, long categoriaId, String nombre, String monto,
                           String periodicidad, int diaPago) throws Exception {
        String r = mockMvc.perform(post("/api/v1/gastos-fijos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"%s","categoriaId":%d,"monto":%s,
                                 "periodicidad":"%s","diaPago":%d}"""
                                .formatted(nombre, categoriaId, monto, periodicidad, diaPago)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r).get("id").asLong();
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
                                {"nombre":"Ahorros","tipo":"AHORROS","saldoInicial":5000000.00}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r).get("id").asLong();
    }

    private void crearTarjeta(String token, String nombre, String deuda, String cupo)
            throws Exception {
        mockMvc.perform(post("/api/v1/cuentas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"%s","tipo":"TARJETA_CREDITO",
                                 "saldoInicial":%s,"cupo":%s}""".formatted(nombre, deuda, cupo)))
                .andExpect(status().isCreated());
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
        JsonNode nodo = objectMapper.readTree(r);
        for (int i = 0; i < nodo.size(); i++) {
            long id = nodo.get(i).get("id").asLong();
            if (id != distintaDe) return id;
        }
        throw new IllegalStateException("Se esperaba mas de una categoria de gasto");
    }

    private void crearPresupuesto(String token, long categoriaId, String limite) throws Exception {
        mockMvc.perform(post("/api/v1/presupuestos").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"categoriaId":%d,"montoLimite":%s,"anio":%d,"mes":%d}"""
                                .formatted(categoriaId, limite,
                                        AHORA.getYear(), AHORA.getMonthValue())))
                .andExpect(status().isCreated());
    }

    /** El tipo del movimiento lo define la categoria (RN-002). */
    private void gastar(String token, long cuenta, long categoria, String monto) throws Exception {
        registrar(token, cuenta, categoria, monto);
    }

    private void ingresar(String token, long cuenta, long categoria, String monto) throws Exception {
        registrar(token, cuenta, categoria, monto);
    }

    private void registrar(String token, long cuenta, long categoria, String monto)
            throws Exception {
        mockMvc.perform(post("/api/v1/transacciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"cuentaId":%d,"categoriaId":%d,"monto":%s,"fecha":"%s"}"""
                                .formatted(cuenta, categoria, monto, LocalDate.now())))
                .andExpect(status().isCreated());
    }
}
