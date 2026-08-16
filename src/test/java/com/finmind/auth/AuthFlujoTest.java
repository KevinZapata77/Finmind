package com.finmind.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finmind.identidad.entity.CodigoVerificacion;
import com.finmind.identidad.repository.CodigoVerificacionRepository;
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

    @Autowired
    private CodigoVerificacionRepository codigoRepository;

    @BeforeEach
    void prepararDatos() {
        codigoRepository.deleteAll();
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
    @DisplayName("registro con datos validos devuelve 201 y la cuenta queda sin verificar")
    void registroValido() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(CORREO)))
                .andExpect(status().isCreated())
                // RN-011: no se emite token al registrarse. El acceso llega tras verificar
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.usuario.correo").value(CORREO))
                .andExpect(jsonPath("$.usuario.rol").value(Rol.USUARIO));

        Usuario creado = usuarioRepository.findByCorreo(CORREO).orElseThrow();
        assertThat(creado.estaVerificado()).isFalse();
    }

    @Test
    @DisplayName("al registrarse se emite un codigo de verificacion de seis digitos")
    void registroEmiteCodigo() throws Exception {
        registrarSinVerificar();
        Usuario creado = usuarioRepository.findByCorreo(CORREO).orElseThrow();
        var codigo = codigoRepository
                .findByUsuarioIdAndTipoAndUsadoEnIsNull(creado.getId(), CodigoVerificacion.VERIFICACION);
        assertThat(codigo).isPresent();
        assertThat(codigo.get().getCodigo()).matches("\\d{6}");
        assertThat(codigo.get().estaVigente()).isTrue();
    }

    @Test
    @DisplayName("acceso denegado: iniciar sesion sin verificar el correo devuelve 403")
    void loginSinVerificar() throws Exception {
        registrarSinVerificar();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(CORREO, CONTRASENA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Debes verificar tu correo antes de iniciar sesion."));
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

    /** Registra la cuenta y la deja sin verificar. */
    private void registrarSinVerificar() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(CORREO)))
                .andExpect(status().isCreated());
    }

    /**
     * Registra, lee el codigo emitido y verifica la cuenta.
     * Devuelve el token, que es lo que el resto de las pruebas necesita.
     */
    private String registrarUsuario() throws Exception {
        registrarSinVerificar();
        Usuario creado = usuarioRepository.findByCorreo(CORREO).orElseThrow();
        String codigo = codigoRepository
                .findByUsuarioIdAndTipoAndUsadoEnIsNull(creado.getId(), CodigoVerificacion.VERIFICACION)
                .orElseThrow().getCodigo();

        String respuesta = mockMvc.perform(post("/api/v1/auth/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","codigo":"%s"}
                                """.formatted(CORREO, codigo)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(respuesta).get("token").asText();
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
