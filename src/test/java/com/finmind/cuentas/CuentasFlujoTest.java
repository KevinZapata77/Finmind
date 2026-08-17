package com.finmind.cuentas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finmind.identidad.entity.CodigoVerificacion;
import com.finmind.identidad.repository.CodigoVerificacionRepository;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.UsuarioRepository;
import com.finmind.soporte.LimpiadorDeDatos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Modulo de cuentas (RF-006 a RF-008).
 *
 * El caso mas importante es aislamientoEntreUsuarios: comprueba RN-005, que es
 * la regla que impide que alguien vea el dinero de otro cambiando un numero
 * en la URL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Cuentas financieras")
class CuentasFlujoTest {

    private static final String CONTRASENA = "ClaveSegura123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CodigoVerificacionRepository codigoRepository;

    @Autowired private LimpiadorDeDatos limpiador;

    @BeforeEach
    void prepararDatos() {
        // Un solo punto de limpieza para todas las pruebas: ver LimpiadorDeDatos.
        limpiador.limpiar();
    }

    // ------------------------------------------------- Crear (RF-006)

    @Test
    @DisplayName("Se crea una cuenta y devuelve 201 con su saldo")
    void seCreaUnaCuenta() throws Exception {
        String token = usuarioListo("ana@finmind.test");

        mockMvc.perform(post("/api/v1/cuentas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ahorros","tipo":"AHORROS","saldoInicial":1500000.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Ahorros"))
                .andExpect(jsonPath("$.tipo").value("AHORROS"))
                .andExpect(jsonPath("$.saldoInicial").value(1500000.00))
                .andExpect(jsonPath("$.saldoActual").value(1500000.00))
                .andExpect(jsonPath("$.moneda").value("COP"))
                .andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    @DisplayName("Sin saldo ni moneda toma los valores por defecto")
    void valoresPorDefecto() throws Exception {
        String token = usuarioListo("ana@finmind.test");

        mockMvc.perform(post("/api/v1/cuentas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Billetera","tipo":"EFECTIVO"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoInicial").value(0))
                .andExpect(jsonPath("$.moneda").value("COP"));
    }

    @Test
    @DisplayName("No se admiten dos cuentas propias con el mismo nombre")
    void nombreRepetidoSeRechaza() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        crearCuenta(token, "Ahorros", "AHORROS");

        mockMvc.perform(post("/api/v1/cuentas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"  ahorros  ","tipo":"CORRIENTE"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Un tipo inventado se rechaza con 400")
    void tipoInvalidoSeRechaza() throws Exception {
        String token = usuarioListo("ana@finmind.test");

        mockMvc.perform(post("/api/v1/cuentas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Cripto","tipo":"BITCOIN"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un saldo negativo se rechaza con 400")
    void saldoNegativoSeRechaza() throws Exception {
        String token = usuarioListo("ana@finmind.test");

        mockMvc.perform(post("/api/v1/cuentas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Deuda","tipo":"OTRO","saldoInicial":-500.00}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Mas de dos decimales se rechaza (RN-010)")
    void demasiadosDecimalesSeRechaza() throws Exception {
        String token = usuarioListo("ana@finmind.test");

        mockMvc.perform(post("/api/v1/cuentas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Rara","tipo":"OTRO","saldoInicial":100.12345}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RF-040: al registrarse ya existe una cuenta de efectivo")
    void naceConCuentaDeEfectivo() throws Exception {
        String token = usuarioListo("ana@finmind.test");

        // Sin esto, el primer gesto de alguien recien registrado seria llenar un
        // formulario de cuentas antes de poder anotar un solo peso.
        mockMvc.perform(get("/api/v1/cuentas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Efectivo"))
                .andExpect(jsonPath("$[0].tipo").value("EFECTIVO"))
                .andExpect(jsonPath("$[0].saldoActual").value(0))
                .andExpect(jsonPath("$[0].activa").value(true));
    }

    // ------------------------------------------------- Listar (RF-007)

    @Test
    @DisplayName("El listado trae solo las cuentas activas por defecto")
    void listadoOmiteInactivas() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long id = crearCuenta(token, "Vieja", "OTRO");
        crearCuenta(token, "Nueva", "AHORROS");
        mockMvc.perform(patch("/api/v1/cuentas/" + id + "/desactivar")
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        // Cuentan tres: la Efectivo que se crea al registrarse, mas las dos de la prueba.
        mockMvc.perform(get("/api/v1/cuentas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.nombre == 'Nueva')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.nombre == 'Vieja')]").isEmpty());

        mockMvc.perform(get("/api/v1/cuentas?incluirInactivas=true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.nombre == 'Vieja')]").isNotEmpty());
    }

    // ------------------------------------------------- RN-005

    @Test
    @DisplayName("RN-005: la cuenta de otro usuario responde 404, no 403")
    void aislamientoEntreUsuarios() throws Exception {
        String tokenAna = usuarioListo("ana@finmind.test");
        long cuentaDeAna = crearCuenta(tokenAna, "Ahorros de Ana", "AHORROS");

        String tokenLuis = usuarioListo("luis@finmind.test");

        // Consultar, editar, desactivar y activar: ninguna deja tocar lo ajeno,
        // y todas responden "no existe" para no confirmar que la cuenta existe.
        mockMvc.perform(get("/api/v1/cuentas/" + cuentaDeAna)
                .header("Authorization", "Bearer " + tokenLuis)).andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/cuentas/" + cuentaDeAna)
                        .header("Authorization", "Bearer " + tokenLuis)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mia ahora","tipo":"OTRO"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/cuentas/" + cuentaDeAna + "/desactivar")
                .header("Authorization", "Bearer " + tokenLuis)).andExpect(status().isNotFound());

        // Luis solo ve SU cuenta de efectivo. La de Ana no aparece por ningun lado.
        mockMvc.perform(get("/api/v1/cuentas").header("Authorization", "Bearer " + tokenLuis))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Efectivo"))
                .andExpect(jsonPath("$[?(@.nombre == 'Ahorros de Ana')]").isEmpty());
    }

    @Test
    @DisplayName("Dos usuarios distintos si pueden usar el mismo nombre de cuenta")
    void elNombreEsUnicoPorUsuarioNoGlobal() throws Exception {
        String tokenAna = usuarioListo("ana@finmind.test");
        crearCuenta(tokenAna, "Ahorros", "AHORROS");

        String tokenLuis = usuarioListo("luis@finmind.test");
        mockMvc.perform(post("/api/v1/cuentas").header("Authorization", "Bearer " + tokenLuis)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ahorros","tipo":"AHORROS"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Sin token no se llega a ninguna ruta de cuentas")
    void sinTokenNoHayCuentas() throws Exception {
        mockMvc.perform(get("/api/v1/cuentas")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/cuentas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"X","tipo":"OTRO"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------- Editar (RF-008)

    @Test
    @DisplayName("Se edita el nombre y el tipo")
    void seEditaLaCuenta() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long id = crearCuenta(token, "Ahorros", "AHORROS");

        mockMvc.perform(put("/api/v1/cuentas/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ahorros Bancolombia","tipo":"CORRIENTE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ahorros Bancolombia"))
                .andExpect(jsonPath("$.tipo").value("CORRIENTE"));
    }

    @Test
    @DisplayName("Al editar, conservar el propio nombre no es conflicto")
    void editarSinCambiarElNombreNoChoca() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long id = crearCuenta(token, "Ahorros", "AHORROS");

        mockMvc.perform(put("/api/v1/cuentas/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ahorros","tipo":"EFECTIVO"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Una cuenta desactivada se puede volver a activar")
    void desactivarYActivar() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long id = crearCuenta(token, "Ahorros", "AHORROS");

        mockMvc.perform(patch("/api/v1/cuentas/" + id + "/desactivar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));

        mockMvc.perform(patch("/api/v1/cuentas/" + id + "/activar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    @DisplayName("Una cuenta que no existe responde 404")
    void cuentaInexistente() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        mockMvc.perform(get("/api/v1/cuentas/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------- apoyo

    /** Registra, verifica el correo y devuelve el token listo para usar. */
    private String usuarioListo(String correo) throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Prueba","apellido":"Usuario","correo":"%s","contrasena":"%s"}
                                """.formatted(correo, CONTRASENA)))
                .andExpect(status().isCreated());

        Usuario creado = usuarioRepository.findByCorreo(correo).orElseThrow();
        String codigo = codigoRepository
                .findByUsuarioIdAndTipoAndUsadoEnIsNull(creado.getId(), CodigoVerificacion.VERIFICACION)
                .orElseThrow().getCodigo();

        String respuesta = mockMvc.perform(post("/api/v1/auth/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","codigo":"%s"}
                                """.formatted(correo, codigo)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(respuesta).get("token").asText();
    }

    private long crearCuenta(String token, String nombre, String tipo) throws Exception {
        String respuesta = mockMvc.perform(post("/api/v1/cuentas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","tipo":"%s"}
                                """.formatted(nombre, tipo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("id").asLong();
    }
}
