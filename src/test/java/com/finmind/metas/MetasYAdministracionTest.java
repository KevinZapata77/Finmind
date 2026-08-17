package com.finmind.metas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finmind.identidad.entity.CodigoVerificacion;
import com.finmind.identidad.repository.CodigoVerificacionRepository;
import com.finmind.soporte.LimpiadorDeDatos;
import com.finmind.usuarios.entity.Rol;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.RolRepository;
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
 * Metas de ahorro y administracion (RF-023, RF-024, RF-032 a RF-034).
 *
 * La prueba mas importante es elAdminNoVeElDineroDeNadie: comprueba que tener
 * rol de administrador no da acceso a la informacion financiera de otros.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Metas de ahorro y administracion")
class MetasYAdministracionTest {

    private static final String CONTRASENA = "ClaveSegura123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private CodigoVerificacionRepository codigoRepository;
    @Autowired private LimpiadorDeDatos limpiador;

    @BeforeEach
    void prepararDatos() {
        limpiador.limpiar();
    }

    // ------------------------------------------------- RF-032

    @Test
    @DisplayName("Se crea una meta y nace en cero")
    void seCreaUnaMeta() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        mockMvc.perform(post("/api/v1/metas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Viaje a Cartagena","montoObjetivo":3000000.00}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montoActual").value(0))
                .andExpect(jsonPath("$.loQueFalta").value(3000000.00))
                .andExpect(jsonPath("$.porcentajeAvance").value(0.0))
                .andExpect(jsonPath("$.estado").value("EN_CURSO"));
    }

    @Test
    @DisplayName("Una fecha limite en el pasado se rechaza")
    void fechaLimitePasada() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        mockMvc.perform(post("/api/v1/metas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Tarde","montoObjetivo":100000.00,"fechaLimite":"%s"}"""
                                .formatted(LocalDate.now().minusDays(1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("No se admiten dos metas con el mismo nombre")
    void nombreRepetido() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        crearMeta(token, "Viaje", "1000000.00");
        mockMvc.perform(post("/api/v1/metas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"viaje","montoObjetivo":500000.00}"""))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------- RF-033 y RN-017

    @Test
    @DisplayName("Al abonar sube el avance, y al llegar al objetivo se completa sola")
    void abonarHastaCompletar() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long meta = crearMeta(token, "Computador", "1000000.00");

        abonar(token, meta, "250000.00")
                .andExpect(jsonPath("$.montoActual").value(250000.00))
                .andExpect(jsonPath("$.porcentajeAvance").value(25.0))
                .andExpect(jsonPath("$.estado").value("EN_CURSO"));

        abonar(token, meta, "750000.00")
                .andExpect(jsonPath("$.montoActual").value(1000000.00))
                .andExpect(jsonPath("$.porcentajeAvance").value(100.0))
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));
    }

    @Test
    @DisplayName("RN-017: una meta completada no admite mas abonos")
    void metaCompletadaNoAdmiteAbonos() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long meta = crearMeta(token, "Chiquita", "100000.00");
        abonar(token, meta, "100000.00").andExpect(jsonPath("$.estado").value("COMPLETADA"));

        mockMvc.perform(post("/api/v1/metas/" + meta + "/abonos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"monto":1000.00}"""))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Un abono mayor que lo que falta se rechaza en vez de recortarse")
    void abonoExcesivo() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long meta = crearMeta(token, "Viaje", "100000.00");
        // Recortarlo en silencio le ocultaria al usuario que se equivoco de cifra.
        mockMvc.perform(post("/api/v1/metas/" + meta + "/abonos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"monto":500000.00}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("No se puede bajar el objetivo por debajo de lo ya ahorrado")
    void objetivoMenorQueLoAhorrado() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long meta = crearMeta(token, "Viaje", "1000000.00");
        abonar(token, meta, "600000.00");

        mockMvc.perform(put("/api/v1/metas/" + meta).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Viaje","montoObjetivo":300000.00}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Una meta cancelada conserva lo ahorrado")
    void cancelarConservaLoAhorrado() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        long meta = crearMeta(token, "Viaje", "1000000.00");
        abonar(token, meta, "400000.00");

        mockMvc.perform(patch("/api/v1/metas/" + meta + "/cancelar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.montoActual").value(400000.00));
    }

    @Test
    @DisplayName("RN-005: la meta de otro usuario responde 404")
    void aislamientoDeMetas() throws Exception {
        String tokenAna = usuarioListo("ana@finmind.test");
        long deAna = crearMeta(tokenAna, "Viaje de Ana", "1000000.00");

        String tokenLuis = usuarioListo("luis@finmind.test");
        mockMvc.perform(get("/api/v1/metas/" + deAna)
                .header("Authorization", "Bearer " + tokenLuis)).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/metas/" + deAna + "/abonos")
                        .header("Authorization", "Bearer " + tokenLuis)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"monto":1000.00}"""))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/metas").header("Authorization", "Bearer " + tokenLuis))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ------------------------------------------------- RF-023 y RF-024

    @Test
    @DisplayName("Un usuario normal no entra a administracion")
    void usuarioNormalNoAdministra() throws Exception {
        String token = usuarioListo("ana@finmind.test");
        mockMvc.perform(get("/api/v1/admin/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/auditoria").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El administrador lista usuarios y ve los conteos")
    void adminListaUsuarios() throws Exception {
        usuarioListo("ana@finmind.test");
        String admin = administradorListo("admin@finmind.test");

        mockMvc.perform(get("/api/v1/admin/usuarios").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // Nunca expone la contrasena
                .andExpect(jsonPath("$[0].contrasenaHash").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/resumen").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.activos").value(2));
    }

    @Test
    @DisplayName("RN-005: el administrador NO ve el dinero de nadie")
    void elAdminNoVeElDineroDeNadie() throws Exception {
        // Ana registra plata.
        String tokenAna = usuarioListo("ana@finmind.test");
        crearMeta(tokenAna, "Viaje de Ana", "1000000.00");
        mockMvc.perform(post("/api/v1/cuentas").header("Authorization", "Bearer " + tokenAna)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Ahorros","tipo":"AHORROS","saldoInicial":9000000.00}"""))
                .andExpect(status().isCreated());

        String admin = administradorListo("admin@finmind.test");

        // El administrador entra a los mismos endpoints financieros: solo ve LO SUYO,
        // que esta vacio. El rol no abre ninguna puerta a los datos de otros.
        mockMvc.perform(get("/api/v1/cuentas").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/metas").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/obligaciones/patrimonio").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.activos").value(0));

        // Y el listado administrativo no trae ni un dato financiero.
        String r = mockMvc.perform(get("/api/v1/admin/usuarios")
                        .header("Authorization", "Bearer " + admin))
                .andReturn().getResponse().getContentAsString();
        for (String prohibido : new String[]{"saldo", "monto", "9000000"}) {
            if (r.toLowerCase().contains(prohibido)) {
                throw new AssertionError("El listado de administracion expuso '" + prohibido + "'");
            }
        }
    }

    @Test
    @DisplayName("RF-024: desactivar deja rastro en la auditoria")
    void desactivarQuedaRegistrado() throws Exception {
        usuarioListo("ana@finmind.test");
        String admin = administradorListo("admin@finmind.test");
        long idAna = usuarioRepository.findByCorreo("ana@finmind.test").orElseThrow().getId();

        mockMvc.perform(patch("/api/v1/admin/usuarios/" + idAna + "/desactivar")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.estado").value("Desactivada"));

        mockMvc.perform(get("/api/v1/admin/auditoria").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accion").value("DESACTIVAR_USUARIO"))
                .andExpect(jsonPath("$[0].adminCorreo").value("admin@finmind.test"))
                .andExpect(jsonPath("$[0].entidadId").value(idAna));
    }

    @Test
    @DisplayName("Un administrador no puede desactivarse a si mismo")
    void adminNoSeDesactivaASiMismo() throws Exception {
        String admin = administradorListo("admin@finmind.test");
        long idAdmin = usuarioRepository.findByCorreo("admin@finmind.test").orElseThrow().getId();
        // Si el ultimo admin se apaga, no queda nadie que pueda reactivarlo.
        mockMvc.perform(patch("/api/v1/admin/usuarios/" + idAdmin + "/desactivar")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Sin token no se llega a metas ni a administracion")
    void sinTokenNada() throws Exception {
        mockMvc.perform(get("/api/v1/metas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/usuarios")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------- apoyo

    private String usuarioListo(String correo) throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Prueba","apellido":"Usuario","correo":"%s","contrasena":"%s"}"""
                                .formatted(correo, CONTRASENA)))
                .andExpect(status().isCreated());
        return verificarYObtenerToken(correo);
    }

    /** Registra y despues le sube el rol, que es como se crea un admin de verdad. */
    private String administradorListo(String correo) throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Admin","apellido":"Plataforma","correo":"%s","contrasena":"%s"}"""
                                .formatted(correo, CONTRASENA)))
                .andExpect(status().isCreated());
        Usuario u = usuarioRepository.findByCorreo(correo).orElseThrow();
        Rol admin = rolRepository.findByNombre(Rol.ADMIN).orElseThrow();
        u.cambiarRol(admin);
        usuarioRepository.save(u);
        return verificarYObtenerToken(correo);
    }

    private String verificarYObtenerToken(String correo) throws Exception {
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

    private long crearMeta(String token, String nombre, String objetivo) throws Exception {
        String r = mockMvc.perform(post("/api/v1/metas").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"%s","montoObjetivo":%s}""".formatted(nombre, objetivo)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(r).get("id").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions abonar(
            String token, long meta, String monto) throws Exception {
        return mockMvc.perform(post("/api/v1/metas/" + meta + "/abonos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"monto":%s}""".formatted(monto)))
                .andExpect(status().isOk());
    }
}
