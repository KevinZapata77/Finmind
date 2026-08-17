package com.finmind.movimientos;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Categorias y movimientos (RF-009 a RF-016).
 *
 * La prueba mas importante es elSaldoDeLaCuentaSeMueve: comprueba que el saldo
 * que muestra el modulo de cuentas refleja de verdad los movimientos, que era
 * la deuda DT-09.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Categorias y movimientos")
class MovimientosFlujoTest {

    private static final String CONTRASENA = "ClaveSegura123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CodigoVerificacionRepository codigoRepository;
    @Autowired private LimpiadorDeDatos limpiador;

    @BeforeEach
    void prepararDatos() {
        limpiador.limpiar();
    }

    // ---------------------------------------------------- Categorias

    @Test
    @DisplayName("Las categorias del sistema estan disponibles sin crear ninguna")
    void lasDelSistemaYaEstan() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        mockMvc.perform(get("/api/v1/categorias").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.delSistema == true)]").isNotEmpty());
    }

    @Test
    @DisplayName("Se puede filtrar por tipo")
    void filtroPorTipo() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        mockMvc.perform(get("/api/v1/categorias?tipo=INGRESO").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tipo == 'GASTO')]").isEmpty());
    }

    @Test
    @DisplayName("No se admite una categoria que choque con una del sistema")
    void chocaConLaDelSistema() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        mockMvc.perform(post("/api/v1/categorias").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Salario","tipo":"INGRESO","colorHex":"#0E8368"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Una categoria del sistema no se puede editar")
    void laDelSistemaNoSeEdita() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        String lista = mockMvc.perform(get("/api/v1/categorias").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        long idSistema = objectMapper.readTree(lista).get(0).get("id").asLong();

        mockMvc.perform(put("/api/v1/categorias/" + idSistema).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mia ahora","colorHex":"#111827"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un color que no es hexadecimal se rechaza")
    void colorInvalido() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        mockMvc.perform(post("/api/v1/categorias").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mascotas","tipo":"GASTO","colorHex":"azul"}"""))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------- RN-002

    @Test
    @DisplayName("RN-002: el tipo lo pone la categoria, no el cliente")
    void elTipoLoDefineLaCategoria() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cuenta = crearCuenta(token, "Ahorros", "1000000.00");
        long catGasto = categoriaDeTipo(token, "GASTO");

        // El cuerpo no lleva "tipo": aunque lo llevara, se ignora.
        mockMvc.perform(post("/api/v1/transacciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"cuentaId":%d,"categoriaId":%d,"monto":50000.00,"fecha":"%s",
                                 "descripcion":"Mercado"}""".formatted(cuenta, catGasto, LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("GASTO"));
    }

    // ---------------------------------------------------- DT-09

    @Test
    @DisplayName("DT-09: el saldo de la cuenta refleja los movimientos")
    void elSaldoDeLaCuentaSeMueve() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cuenta = crearCuenta(token, "Ahorros", "1000000.00");
        long ingreso = categoriaDeTipo(token, "INGRESO");
        long gasto = categoriaDeTipo(token, "GASTO");

        registrar(token, cuenta, ingreso, "500000.00");
        registrar(token, cuenta, gasto, "200000.00");

        // 1.000.000 + 500.000 - 200.000 = 1.300.000
        mockMvc.perform(get("/api/v1/cuentas/" + cuenta).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoInicial").value(1000000.00))
                .andExpect(jsonPath("$.saldoActual").value(1300000.00));

        // Y el patrimonio usa el saldo real, no el inicial.
        mockMvc.perform(get("/api/v1/obligaciones/patrimonio").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.activos").value(1300000.00));
    }

    @Test
    @DisplayName("Al borrar un movimiento el saldo vuelve atras")
    void borrarDevuelveElSaldo() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cuenta = crearCuenta(token, "Ahorros", "1000000.00");
        long gasto = categoriaDeTipo(token, "GASTO");
        long mov = registrar(token, cuenta, gasto, "300000.00");

        mockMvc.perform(get("/api/v1/cuentas/" + cuenta).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.saldoActual").value(700000.00));

        mockMvc.perform(delete("/api/v1/transacciones/" + mov).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/cuentas/" + cuenta).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.saldoActual").value(1000000.00));
    }

    // ---------------------------------------------------- Filtros y totales

    @Test
    @DisplayName("Los totales corresponden al filtro completo, no a la pagina")
    void totalesDelFiltroNoDeLaPagina() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cuenta = crearCuenta(token, "Ahorros", "0.00");
        long ingreso = categoriaDeTipo(token, "INGRESO");
        long gasto = categoriaDeTipo(token, "GASTO");
        for (int i = 0; i < 3; i++) registrar(token, cuenta, ingreso, "100000.00");
        for (int i = 0; i < 2; i++) registrar(token, cuenta, gasto, "40000.00");

        mockMvc.perform(get("/api/v1/transacciones?size=2").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(2))
                .andExpect(jsonPath("$.totalElementos").value(5))
                .andExpect(jsonPath("$.totalIngresos").value(300000.00))
                .andExpect(jsonPath("$.totalGastos").value(80000.00))
                .andExpect(jsonPath("$.diferencia").value(220000.00));
    }

    @Test
    @DisplayName("Una fecha futura se rechaza")
    void fechaFuturaSeRechaza() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cuenta = crearCuenta(token, "Ahorros", "0.00");
        long cat = categoriaDeTipo(token, "GASTO");
        mockMvc.perform(post("/api/v1/transacciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"cuentaId":%d,"categoriaId":%d,"monto":1000.00,"fecha":"%s"}"""
                                .formatted(cuenta, cat, LocalDate.now().plusDays(1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RN-001: un monto negativo se rechaza")
    void montoNegativoSeRechaza() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cuenta = crearCuenta(token, "Ahorros", "0.00");
        long cat = categoriaDeTipo(token, "GASTO");
        mockMvc.perform(post("/api/v1/transacciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"cuentaId":%d,"categoriaId":%d,"monto":-1000.00,"fecha":"%s"}"""
                                .formatted(cuenta, cat, LocalDate.now())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("No se registra en una cuenta desactivada")
    void cuentaDesactivadaNoRecibeMovimientos() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long cuenta = crearCuenta(token, "Vieja", "0.00");
        long cat = categoriaDeTipo(token, "GASTO");
        mockMvc.perform(patch("/api/v1/cuentas/" + cuenta + "/desactivar")
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/transacciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"cuentaId":%d,"categoriaId":%d,"monto":1000.00,"fecha":"%s"}"""
                                .formatted(cuenta, cat, LocalDate.now())))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------- RN-005

    @Test
    @DisplayName("RN-005: no se puede usar la cuenta de otro usuario")
    void noSePuedeUsarLaCuentaAjena() throws Exception {
        String tokenAna = usuarioListo("ana@finmind.test");
        long cuentaDeAna = crearCuenta(tokenAna, "Ahorros de Ana", "1000000.00");

        String tokenLuis = usuarioListo("luis@finmind.test");
        long catLuis = categoriaDeTipo(tokenLuis, "GASTO");

        mockMvc.perform(post("/api/v1/transacciones").header("Authorization", "Bearer " + tokenLuis)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"cuentaId":%d,"categoriaId":%d,"monto":1000.00,"fecha":"%s"}"""
                                .formatted(cuentaDeAna, catLuis, LocalDate.now())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/transacciones").header("Authorization", "Bearer " + tokenLuis))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    @DisplayName("Sin token no se llega a movimientos ni a categorias")
    void sinTokenNada() throws Exception {
        mockMvc.perform(get("/api/v1/transacciones")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/categorias")).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------- apoyo

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

    private long crearCuenta(String token, String nombre, String saldo) throws Exception {
        String r = mockMvc.perform(post("/api/v1/cuentas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"%s","tipo":"AHORROS","saldoInicial":%s}"""
                                .formatted(nombre, saldo)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r).get("id").asLong();
    }

    /** Toma la primera categoria del sistema del tipo pedido. */
    private long categoriaDeTipo(String token, String tipo) throws Exception {
        String r = mockMvc.perform(get("/api/v1/categorias?tipo=" + tipo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r).get(0).get("id").asLong();
    }

    private long registrar(String token, long cuenta, long categoria, String monto) throws Exception {
        String r = mockMvc.perform(post("/api/v1/transacciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"cuentaId":%d,"categoriaId":%d,"monto":%s,"fecha":"%s"}"""
                                .formatted(cuenta, categoria, monto, LocalDate.now())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r).get("id").asLong();
    }
}
