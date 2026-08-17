package com.finmind.obligaciones;

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
 * Obligaciones financieras (RF-035 a RF-039).
 *
 * El nucleo de estas pruebas es la aritmetica: que cada pago se parta bien entre
 * interes y capital (RN-018) y que un pago corto no simule un avance (RN-019).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Obligaciones financieras")
class ObligacionesFlujoTest {

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

    // ------------------------------------------------- RF-035

    @Test
    @DisplayName("Se registra una obligacion y nace debiendo todo")
    void seRegistraUnaObligacion() throws Exception {
        String token = usuarioListo("ana@finmind.test");

        mockMvc.perform(post("/api/v1/obligaciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Tarjeta Visa","acreedor":"Bancolombia","tipo":"TARJETA_CREDITO",
                                 "montoOriginal":2400000.00,"tasaAnual":24.0000,"cuotaMensual":300000.00,
                                 "diaPago":15,"fechaInicio":"2026-01-15"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoPendiente").value(2400000.00))
                .andExpect(jsonPath("$.porcentajePagado").value(0.0))
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                // 2.400.000 x 24% / 12 = 48.000
                .andExpect(jsonPath("$.interesDelPeriodo").value(48000.00));
    }

    @Test
    @DisplayName("Una tasa de 2400 en vez de 24 se rechaza")
    void tasaDesproporcionadaSeRechaza() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        mockMvc.perform(post("/api/v1/obligaciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Error","acreedor":"X","tipo":"OTRO","montoOriginal":100000.00,
                                 "tasaAnual":2400.0,"cuotaMensual":10000.00,"diaPago":5,
                                 "fechaInicio":"2026-01-05"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un dia de pago 31 se rechaza: no existe en todos los meses")
    void diaDePagoFueraDeRango() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        mockMvc.perform(post("/api/v1/obligaciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Prestamo","acreedor":"X","tipo":"OTRO","montoOriginal":100000.00,
                                 "tasaAnual":0,"cuotaMensual":10000.00,"diaPago":31,
                                 "fechaInicio":"2026-01-05"}"""))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------- RN-018

    @Test
    @DisplayName("RN-018: el pago se parte entre interes y capital")
    void elPagoSeDescompone() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long id = crearDeuda(token, "Tarjeta", "2400000.00", "24.0000", "300000.00");

        // Interes = 2.400.000 x 24% / 12 = 48.000. Capital = 300.000 - 48.000 = 252.000
        mockMvc.perform(post("/api/v1/obligaciones/" + id + "/pagos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"monto":300000.00,"fecha":"%s"}""".formatted(LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interes").value(48000.00))
                .andExpect(jsonPath("$.abonoCapital").value(252000.00))
                .andExpect(jsonPath("$.saldoResultante").value(2148000.00))
                .andExpect(jsonPath("$.advertencia").doesNotExist());
    }

    @Test
    @DisplayName("Sin interes, todo el pago va a capital")
    void tasaCeroAbonaTodoACapital() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long id = crearDeuda(token, "Prestamo de mi tia", "500000.00", "0", "100000.00");

        mockMvc.perform(post("/api/v1/obligaciones/" + id + "/pagos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"monto":100000.00,"fecha":"%s"}""".formatted(LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interes").value(0.00))
                .andExpect(jsonPath("$.abonoCapital").value(100000.00))
                .andExpect(jsonPath("$.saldoResultante").value(400000.00));
    }

    // ------------------------------------------------- RN-019

    @Test
    @DisplayName("RN-019: un pago que no cubre el interes no baja la deuda, y lo avisa")
    void pagoCortoNoReduceLaDeuda() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        // Interes = 2.400.000 x 24% / 12 = 48.000. Se paga menos que eso.
        long id = crearDeuda(token, "Tarjeta", "2400000.00", "24.0000", "300000.00");

        mockMvc.perform(post("/api/v1/obligaciones/" + id + "/pagos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"monto":30000.00,"fecha":"%s"}""".formatted(LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interes").value(30000.00))
                .andExpect(jsonPath("$.abonoCapital").value(0.00))
                .andExpect(jsonPath("$.saldoResultante").value(2400000.00))
                .andExpect(jsonPath("$.advertencia").exists());

        // Y la deuda quedo igual que antes del pago.
        mockMvc.perform(get("/api/v1/obligaciones/" + id).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.saldoPendiente").value(2400000.00));
    }

    // ------------------------------------------------- cierre

    @Test
    @DisplayName("Al saldarla queda PAGADA y no admite mas pagos")
    void alSaldarQuedaPagada() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long id = crearDeuda(token, "Chiquita", "100000.00", "0", "100000.00");

        mockMvc.perform(post("/api/v1/obligaciones/" + id + "/pagos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"monto":100000.00,"fecha":"%s"}""".formatted(LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoResultante").value(0.00));

        mockMvc.perform(get("/api/v1/obligaciones/" + id).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.estado").value("PAGADA"))
                .andExpect(jsonPath("$.porcentajePagado").value(100.0));

        mockMvc.perform(post("/api/v1/obligaciones/" + id + "/pagos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"monto":1000.00,"fecha":"%s"}""".formatted(LocalDate.now())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Un pago mayor que la deuda se rechaza")
    void pagoMayorQueLaDeuda() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long id = crearDeuda(token, "Chiquita", "100000.00", "0", "50000.00");

        mockMvc.perform(post("/api/v1/obligaciones/" + id + "/pagos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"monto":150000.00,"fecha":"%s"}""".formatted(LocalDate.now())))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------- RN-020 y RF-038

    @Test
    @DisplayName("RN-020: la tarjeta de credito no cuenta como activo")
    void tarjetaNoEsActivo() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        crearCuenta(token, "Ahorros", "AHORROS", "1000000.00");
        crearCuenta(token, "Visa", "TARJETA_CREDITO", "800000.00");

        // El activo es solo la cuenta de ahorros, no la tarjeta.
        mockMvc.perform(get("/api/v1/obligaciones/patrimonio").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activos").value(1000000.00))
                .andExpect(jsonPath("$.obligaciones").value(0.00))
                .andExpect(jsonPath("$.patrimonioNeto").value(1000000.00));
    }

    @Test
    @DisplayName("RF-038: con mas deuda que activos el patrimonio es negativo y se explica con texto")
    void patrimonioNegativoSeExplica() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        crearCuenta(token, "Ahorros", "AHORROS", "500000.00");
        crearDeuda(token, "Hipoteca", "3000000.00", "0", "200000.00");

        mockMvc.perform(get("/api/v1/obligaciones/patrimonio").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activos").value(500000.00))
                .andExpect(jsonPath("$.obligaciones").value(3000000.00))
                .andExpect(jsonPath("$.patrimonioNeto").value(-2500000.00))
                // El color no puede ser el unico indicador de algo tan importante.
                .andExpect(jsonPath("$.lectura").value("Debes mas de lo que tienes. El patrimonio neto es negativo."));
    }

    // ------------------------------------------------- RN-005

    @Test
    @DisplayName("RN-005: la obligacion de otro usuario responde 404")
    void aislamientoEntreUsuarios() throws Exception {
        String tokenAna = usuarioListo("ana@finmind.test");
        long deAna = crearDeuda(tokenAna, "Tarjeta de Ana", "1000000.00", "12.0000", "100000.00");

        String tokenLuis = usuarioListo("luis@finmind.test");
        mockMvc.perform(get("/api/v1/obligaciones/" + deAna)
                .header("Authorization", "Bearer " + tokenLuis)).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/obligaciones/" + deAna + "/pagos")
                        .header("Authorization", "Bearer " + tokenLuis)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"monto":1000.00,"fecha":"%s"}""".formatted(LocalDate.now())))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/obligaciones/" + deAna + "/pagos")
                .header("Authorization", "Bearer " + tokenLuis)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/obligaciones").header("Authorization", "Bearer " + tokenLuis))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Sin token no se llega a ninguna ruta de obligaciones")
    void sinTokenNoHayObligaciones() throws Exception {
        mockMvc.perform(get("/api/v1/obligaciones")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/obligaciones/patrimonio")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El historial guarda el desglose de cada pago")
    void historialConservaElDesglose() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long id = crearDeuda(token, "Tarjeta", "1200000.00", "12.0000", "200000.00");

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/obligaciones/" + id + "/pagos")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"monto":200000.00,"fecha":"%s"}""".formatted(LocalDate.now())))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(get("/api/v1/obligaciones/" + id + "/pagos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // Primer pago: interes = 1.200.000 x 12% / 12 = 12.000
                .andExpect(jsonPath("$[1].interes").value(12000.00))
                .andExpect(jsonPath("$[1].abonoCapital").value(188000.00));
    }

    // ------------------------------------------------- apoyo

    private String usuarioListo(String correo) throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Prueba","apellido":"Usuario","correo":"%s","contrasena":"%s"}
                                """.formatted(correo, CONTRASENA)))
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

    private long crearDeuda(String token, String nombre, String monto, String tasa, String cuota)
            throws Exception {
        String r = mockMvc.perform(post("/api/v1/obligaciones")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"%s","acreedor":"Banco","tipo":"OTRO","montoOriginal":%s,
                                 "tasaAnual":%s,"cuotaMensual":%s,"diaPago":10,"fechaInicio":"2026-01-10"}
                                """.formatted(nombre, monto, tasa, cuota)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r).get("id").asLong();
    }

    private void crearCuenta(String token, String nombre, String tipo, String saldo) throws Exception {
        mockMvc.perform(post("/api/v1/cuentas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"%s","tipo":"%s","saldoInicial":%s}
                                """.formatted(nombre, tipo, saldo)))
                .andExpect(status().isCreated());
    }
}
