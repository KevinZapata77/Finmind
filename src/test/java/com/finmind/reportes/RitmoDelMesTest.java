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
 * RF-048. El acumulado dia por dia que alimenta la curva del panel.
 *
 * SOBRE EL MES QUE SE USA
 * Casi todo se prueba contra el mes ANTERIOR, no contra el actual. Un mes
 * cerrado tiene todos sus dias, asi que se puede poner un gasto el dia 2 y otro
 * el dia 5 con la seguridad de que existen. Contra el mes en curso, esa misma
 * prueba fallaria cualquier dia 1 o 2 — y una prueba que solo pasa a mitad de
 * mes no prueba nada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Ritmo del mes")
class RitmoDelMesTest {

    private static final String CONTRASENA = "ClaveSegura123";
    private static final YearMonth MES_PASADO = YearMonth.now().minusMonths(1);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CodigoVerificacionRepository codigoRepository;
    @Autowired private LimpiadorDeDatos limpiador;

    @BeforeEach
    void prepararDatos() {
        limpiador.limpiar();
    }

    /**
     * Lo que el nombre promete: cada punto es el acumulado al cierre de ese dia,
     * no el movimiento de ese dia. Si fuera lo segundo, la linea del grafico
     * bajaria despues de un dia de mucho gasto en vez de seguir subiendo.
     */
    @Test
    @DisplayName("Cada punto es el acumulado hasta ese dia, no el gasto de ese dia")
    void elAcumuladoSeArrastra() throws Exception {
        String token = usuarioListo("ritmo@finmind.test");
        long cuenta = crearCuenta(token);
        long gasto = categoria(token, "GASTO");

        gastarEnFecha(token, cuenta, gasto, "100000.00", MES_PASADO.atDay(2));
        gastarEnFecha(token, cuenta, gasto, "50000.00", MES_PASADO.atDay(5));

        JsonNode r = ritmo(token, MES_PASADO);
        JsonNode dias = r.get("dias");

        // Un mes cerrado se dibuja completo.
        assertThat(dias.size()).isEqualTo(MES_PASADO.lengthOfMonth());
        assertThat(r.get("diasTranscurridos").asInt()).isEqualTo(MES_PASADO.lengthOfMonth());

        assertThat(acumulado(dias, 1)).isZero();
        assertThat(acumulado(dias, 2)).isEqualTo(100000.00);
        // Dias 3 y 4 sin movimientos: el acumulado NO vuelve a cero.
        assertThat(acumulado(dias, 3)).isEqualTo(100000.00);
        assertThat(acumulado(dias, 4)).isEqualTo(100000.00);
        assertThat(acumulado(dias, 5)).isEqualTo(150000.00);
        // Y se mantiene hasta el final del mes.
        assertThat(acumulado(dias, MES_PASADO.lengthOfMonth())).isEqualTo(150000.00);
        assertThat(r.get("totalGastos").asDouble()).isEqualTo(150000.00);
    }

    /**
     * Los dias sin movimientos vienen igual. Omitirlos uniria el dia 2 con el 5
     * y el tramo plano — tres dias sin gastar, que es informacion — se perderia.
     */
    @Test
    @DisplayName("Los dias sin movimientos vienen en la serie, no se omiten")
    void losDiasVaciosNoSeSaltan() throws Exception {
        String token = usuarioListo("ritmo@finmind.test");
        long cuenta = crearCuenta(token);
        long gasto = categoria(token, "GASTO");
        gastarEnFecha(token, cuenta, gasto, "80000.00", MES_PASADO.atDay(10));

        JsonNode dias = ritmo(token, MES_PASADO).get("dias");

        // La serie es continua: el dia n esta en la posicion n-1, sin huecos.
        for (int i = 0; i < dias.size(); i++) {
            assertThat(dias.get(i).get("dia").asInt()).isEqualTo(i + 1);
        }
    }

    @Test
    @DisplayName("Los ingresos y los gastos se acumulan por separado")
    void ingresoYGastoNoSeMezclan() throws Exception {
        String token = usuarioListo("ritmo@finmind.test");
        long cuenta = crearCuenta(token);
        long gasto = categoria(token, "GASTO");
        long salario = categoria(token, "INGRESO");

        registrarEnFecha(token, cuenta, salario, "2000000.00", MES_PASADO.atDay(1));
        gastarEnFecha(token, cuenta, gasto, "300000.00", MES_PASADO.atDay(3));

        JsonNode r = ritmo(token, MES_PASADO);
        JsonNode dias = r.get("dias");

        assertThat(dias.get(0).get("ingresoAcumulado").asDouble()).isEqualTo(2000000.00);
        assertThat(dias.get(0).get("gastoAcumulado").asDouble()).isZero();
        assertThat(acumulado(dias, 3)).isEqualTo(300000.00);
        assertThat(r.get("totalIngresos").asDouble()).isEqualTo(2000000.00);
        assertThat(r.get("totalGastos").asDouble()).isEqualTo(300000.00);
    }

    /**
     * Varios movimientos el mismo dia se suman en un punto. La consulta agrupa
     * por fecha y tipo, asi que dos gastos del mismo dia llegan en una sola
     * fila; conviene comprobar que no se pierde ninguno.
     */
    @Test
    @DisplayName("Varios gastos del mismo dia caen en el mismo punto")
    void variosGastosDelMismoDiaSeSuman() throws Exception {
        String token = usuarioListo("ritmo@finmind.test");
        long cuenta = crearCuenta(token);
        long gasto = categoria(token, "GASTO");

        gastarEnFecha(token, cuenta, gasto, "30000.00", MES_PASADO.atDay(7));
        gastarEnFecha(token, cuenta, gasto, "20000.00", MES_PASADO.atDay(7));

        assertThat(acumulado(ritmo(token, MES_PASADO).get("dias"), 7)).isEqualTo(50000.00);
    }

    /** Un mes cerrado no se proyecta: no hay futuro que adivinar. */
    @Test
    @DisplayName("Un mes que ya termino no trae proyeccion")
    void elMesCerradoNoSeProyecta() throws Exception {
        String token = usuarioListo("ritmo@finmind.test");
        long cuenta = crearCuenta(token);
        gastarEnFecha(token, cuenta, categoria(token, "GASTO"), "100000.00", MES_PASADO.atDay(2));

        assertThat(ritmo(token, MES_PASADO).get("proyeccionGasto").isNull()).isTrue();
    }

    /**
     * En el mes en curso la serie llega hasta hoy y no mas. Pintarla hasta el 31
     * dejaria veintidos dias planos y daria a entender que el usuario dejo de
     * gastar.
     */
    @Test
    @DisplayName("En el mes en curso la serie termina hoy, no a fin de mes")
    void elMesEnCursoSeCortaHoy() throws Exception {
        String token = usuarioListo("ritmo@finmind.test");
        LocalDate hoy = LocalDate.now();
        YearMonth mes = YearMonth.from(hoy);

        JsonNode r = ritmo(token, mes);
        assertThat(r.get("dias").size()).isEqualTo(hoy.getDayOfMonth());
        assertThat(r.get("diasTranscurridos").asInt()).isEqualTo(hoy.getDayOfMonth());
        assertThat(r.get("diasDelMes").asInt()).isEqualTo(mes.lengthOfMonth());

        // La proyeccion sigue la misma regla que las alertas: no antes del dia 5.
        if (hoy.getDayOfMonth() < 5) {
            assertThat(r.get("proyeccionGasto").isNull()).isTrue();
        } else {
            assertThat(r.get("proyeccionGasto").isNull()).isFalse();
        }
    }

    @Test
    @DisplayName("Un usuario sin movimientos recibe la serie en ceros, no una lista vacia")
    void sinMovimientosLaSerieSigueExistiendo() throws Exception {
        String token = usuarioListo("vacio@finmind.test");

        JsonNode r = ritmo(token, MES_PASADO);
        // La lista vacia obligaria al cliente a decidir que dibujar. Con ceros,
        // el grafico se pinta plano y se entiende solo.
        assertThat(r.get("dias").size()).isEqualTo(MES_PASADO.lengthOfMonth());
        assertThat(r.get("totalGastos").asDouble()).isZero();
        assertThat(r.get("totalIngresos").asDouble()).isZero();
    }

    /** RN-005: la curva de uno no incluye los movimientos de otro. */
    @Test
    @DisplayName("RN-005: la serie solo cuenta los movimientos del dueno del token")
    void laSerieNoSeMezcla() throws Exception {
        String ana = usuarioListo("ana.ritmo@finmind.test");
        String beto = usuarioListo("beto.ritmo@finmind.test");
        long cuenta = crearCuenta(ana);
        gastarEnFecha(ana, cuenta, categoria(ana, "GASTO"), "400000.00", MES_PASADO.atDay(4));

        assertThat(ritmo(beto, MES_PASADO).get("totalGastos").asDouble()).isZero();
        assertThat(ritmo(ana, MES_PASADO).get("totalGastos").asDouble()).isEqualTo(400000.00);
    }

    @Test
    @DisplayName("El ritmo exige sesion")
    void exigeToken() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/ritmo")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ apoyo

    /** Gasto acumulado del dia n, buscandolo por su numero y no por su posicion. */
    private double acumulado(JsonNode dias, int dia) {
        for (JsonNode d : dias) {
            if (d.get("dia").asInt() == dia) return d.get("gastoAcumulado").asDouble();
        }
        throw new IllegalStateException("No vino el dia " + dia + " en la serie");
    }

    private JsonNode ritmo(String token, YearMonth mes) throws Exception {
        String r = mockMvc.perform(get("/api/v1/reportes/ritmo?anio=%d&mes=%d"
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
