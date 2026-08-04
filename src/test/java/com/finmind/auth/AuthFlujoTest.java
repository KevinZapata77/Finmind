package com.finmind.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion del flujo de autenticacion.
 *
 * Cubre casos positivos y negativos (TST-05) y accesos permitidos y denegados
 * (TST-06). El objetivo no es cobertura: es que estas reglas fallen de forma
 * detectable si alguien las rompe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Flujo de autenticacion")
class AuthFlujoTest {

    private static final String CORREO = "kevin.prueba@finmind.test";
    private static final String CONTRASENA = "ClaveSegura123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @BeforeEach
    void prepararDatos() {
        usuarioRepository.deleteAll();
        if (rolRepository.findByNombre(Rol.USUARIO).isEmpty()) {
            rolRepository.save(new Rol(Rol.USUARIO, "Usuario final"));
        }
        if (rolRepository.findByNombre(Rol.ADMIN).isEmpty()) {
            rolRepository.save(new Rol(Rol.ADMIN, "Administrador de plataforma"));
        }
    }

    // ------------------------------------------------------------------ registro

    @Test
    @DisplayName("registro con datos validos devuelve 201 y un token")
    void registroValido() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(CORREO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.correo").value(CORREO))
                .andExpect(jsonPath("$.usuario.rol").value(Rol.USUARIO));
    }

    @Test
    @DisplayName("registro con correo repetido devuelve 409")
    void registroCorreoDuplicado() throws Exception {
        registrarUsuario();

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(CORREO)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("registro con correo mal formado devuelve 400 y detalla el campo")
    void registroCorreoInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("esto-no-es-un-correo")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.correo").isNotEmpty());
    }

    @Test
    @DisplayName("registro con contrasena de menos de 8 caracteres devuelve 400")
    void registroContrasenaCorta() throws Exception {
        String cuerpo = """
                {"nombre":"Kevin","apellido":"Zapata","correo":"otro@finmind.test","contrasena":"corta"}
                """;

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.contrasena").isNotEmpty());
    }

    @Test
    @DisplayName("la contrasena nunca se almacena en texto plano")
    void contrasenaSeGuardaHasheada() throws Exception {
        registrarUsuario();

        Usuario guardado = usuarioRepository.findByCorreo(CORREO).orElseThrow();

        assertThat(guardado.getContrasenaHash()).isNotEqualTo(CONTRASENA);
        assertThat(guardado.getContrasenaHash()).startsWith("$2");
        assertThat(guardado.getContrasenaHash()).doesNotContain(CONTRASENA);
    }

    // --------------------------------------------------------------------- login

    @Test
    @DisplayName("login con credenciales validas devuelve 200 y un token")
    void loginValido() throws Exception {
        registrarUsuario();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(CORREO, CONTRASENA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.correo").value(CORREO));
    }

    @Test
    @DisplayName("login con contrasena incorrecta devuelve 401 sin revelar la causa")
    void loginContrasenaIncorrecta() throws Exception {
        registrarUsuario();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(CORREO, "ClaveEquivocada999")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contrasena incorrectos"));
    }

    @Test
    @DisplayName("login con correo inexistente devuelve el mismo 401 que una contrasena mala")
    void loginCorreoInexistente() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("nadie@finmind.test", CONTRASENA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contrasena incorrectos"));
    }

    // ------------------------------------------------- acceso permitido y negado

    @Test
    @DisplayName("acceso denegado: /usuarios/me sin token devuelve 401")
    void perfilSinToken() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("acceso denegado: /usuarios/me con token alterado devuelve 401")
    void perfilConTokenAlterado() throws Exception {
        String token = registrarUsuario();
        String alterado = token.substring(0, token.length() - 3) + "abc";

        mockMvc.perform(get("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer " + alterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("acceso permitido: /usuarios/me con token valido devuelve el perfil propio")
    void perfilConTokenValido() throws Exception {
        String token = registrarUsuario();

        mockMvc.perform(get("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(CORREO))
                .andExpect(jsonPath("$.rol").value(Rol.USUARIO));
    }

    @Test
    @DisplayName("la respuesta del perfil no expone el hash de la contrasena")
    void perfilNoExponeHash() throws Exception {
        String token = registrarUsuario();

        String respuesta = mockMvc.perform(get("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(respuesta).doesNotContain("contrasena");
        assertThat(respuesta).doesNotContain("$2");
    }

    // ------------------------------------------------------------------- apoyo

    private String registrarUsuario() throws Exception {
        String respuesta = mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(CORREO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(respuesta);
        return json.get("token").asText();
    }

    private String cuerpoRegistro(String correo) {
        return """
                {"nombre":"Kevin","apellido":"Zapata","correo":"%s","contrasena":"%s"}
                """.formatted(correo, CONTRASENA);
    }

    private String cuerpoLogin(String correo, String contrasena) {
        return """
                {"correo":"%s","contrasena":"%s"}
                """.formatted(correo, contrasena);
    }
}
