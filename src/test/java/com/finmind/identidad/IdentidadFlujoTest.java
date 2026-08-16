package com.finmind.identidad;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verificacion de correo y recuperacion de contrasena (RF-025 a RF-028).
 *
 * Cubre casos de exito, de error y de limite, y en particular la regla
 * RN-014: la respuesta de recuperacion es identica exista o no el correo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Verificacion de identidad")
class IdentidadFlujoTest {

    private static final String CORREO = "ana.prueba@finmind.test";
    private static final String CONTRASENA = "ClaveSegura123";
    private static final String NUEVA = "OtraClaveSegura456";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private CodigoVerificacionRepository codigoRepository;

    @BeforeEach
    void prepararDatos() {
        codigoRepository.deleteAll();
        usuarioRepository.deleteAll();
        if (rolRepository.findByNombre(Rol.USUARIO).isEmpty()) {
            rolRepository.save(new Rol(Rol.USUARIO, "Usuario final"));
        }
    }

    // ------------------------------------------------------ verificacion

    @Test
    @DisplayName("con el codigo correcto la cuenta queda verificada y devuelve token")
    void verificacionExitosa() throws Exception {
        registrar();
        String codigo = codigoVigente(CodigoVerificacion.VERIFICACION);

        mockMvc.perform(post("/api/v1/auth/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoVerificar(codigo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        assertThat(usuarioRepository.findByCorreo(CORREO).orElseThrow().estaVerificado()).isTrue();
    }

    @Test
    @DisplayName("un codigo equivocado se rechaza y no verifica la cuenta")
    void codigoEquivocado() throws Exception {
        registrar();
        mockMvc.perform(post("/api/v1/auth/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoVerificar("000000")))
                .andExpect(status().isBadRequest());

        assertThat(usuarioRepository.findByCorreo(CORREO).orElseThrow().estaVerificado()).isFalse();
    }

    @Test
    @DisplayName("un codigo ya usado no sirve una segunda vez")
    void codigoDeUnSoloUso() throws Exception {
        registrar();
        String codigo = codigoVigente(CodigoVerificacion.VERIFICACION);

        mockMvc.perform(post("/api/v1/auth/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoVerificar(codigo)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoVerificar(codigo)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("reenviar el codigo invalida el anterior")
    void reenviarInvalidaElAnterior() throws Exception {
        registrar();
        String primero = codigoVigente(CodigoVerificacion.VERIFICACION);

        mockMvc.perform(post("/api/v1/auth/reenviar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s"}
                                """.formatted(CORREO)))
                .andExpect(status().isAccepted());

        String segundo = codigoVigente(CodigoVerificacion.VERIFICACION);
        assertThat(segundo).isNotEqualTo(primero);

        // El primero ya no sirve
        mockMvc.perform(post("/api/v1/auth/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoVerificar(primero)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("el codigo emitido tiene seis digitos y esta vigente")
    void formatoDelCodigo() throws Exception {
        registrar();
        Usuario u = usuarioRepository.findByCorreo(CORREO).orElseThrow();
        CodigoVerificacion c = codigoRepository
                .findByUsuarioIdAndTipoAndUsadoEnIsNull(u.getId(), CodigoVerificacion.VERIFICACION)
                .orElseThrow();
        assertThat(c.getCodigo()).matches("\\d{6}");
        assertThat(c.estaVigente()).isTrue();
        assertThat(c.getIntentos()).isZero();
    }

    // ---------------------------------------------------- recuperacion

    @Test
    @DisplayName("la recuperacion responde igual exista o no el correo (RN-014)")
    void respuestaUniformeEnRecuperacion() throws Exception {
        registrar();

        String conCuenta = mockMvc.perform(post("/api/v1/auth/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s"}
                                """.formatted(CORREO)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        String sinCuenta = mockMvc.perform(post("/api/v1/auth/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"nadie@finmind.test"}
                                """))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        // Distinguirlas permitiria averiguar que correos estan registrados
        assertThat(conCuenta).isEqualTo(sinCuenta);
    }

    @Test
    @DisplayName("con el codigo de recuperacion la contrasena cambia y la anterior deja de servir")
    void restablecerCambiaLaContrasena() throws Exception {
        registrar();
        verificarCuenta();

        mockMvc.perform(post("/api/v1/auth/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s"}
                                """.formatted(CORREO)))
                .andExpect(status().isAccepted());

        String codigo = codigoVigente(CodigoVerificacion.RECUPERACION);

        mockMvc.perform(post("/api/v1/auth/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","codigo":"%s","contrasenaNueva":"%s"}
                                """.formatted(CORREO, codigo, NUEVA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        // La contrasena vieja ya no sirve
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","contrasena":"%s"}
                                """.formatted(CORREO, CONTRASENA)))
                .andExpect(status().isUnauthorized());

        // La nueva si
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","contrasena":"%s"}
                                """.formatted(CORREO, NUEVA)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("restablecer con un codigo equivocado no cambia la contrasena")
    void restablecerConCodigoEquivocado() throws Exception {
        registrar();
        verificarCuenta();

        mockMvc.perform(post("/api/v1/auth/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","codigo":"999999","contrasenaNueva":"%s"}
                                """.formatted(CORREO, NUEVA)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","contrasena":"%s"}
                                """.formatted(CORREO, CONTRASENA)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("una contrasena nueva de menos de 8 caracteres se rechaza")
    void contrasenaNuevaCorta() throws Exception {
        registrar();
        mockMvc.perform(post("/api/v1/auth/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","codigo":"123456","contrasenaNueva":"corta"}
                                """.formatted(CORREO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.contrasenaNueva").isNotEmpty());
    }

    @Test
    @DisplayName("el registro acepta el token de CAPTCHA en el cuerpo de la peticion")
    void registroAceptaTokenCaptcha() throws Exception {
        // En pruebas el CAPTCHA esta deshabilitado, asi que el token se ignora.
        // Lo que se verifica aqui es que el contrato acepta el campo sin romperse.
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Restrepo","correo":"otra@finmind.test",
                                 "contrasena":"ClaveSegura123","captchaToken":"token-de-prueba"}
                                """))
                .andExpect(status().isCreated());
    }

    // ------------------------------------------------------------ apoyo

    private void registrar() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Restrepo","correo":"%s","contrasena":"%s"}
                                """.formatted(CORREO, CONTRASENA)))
                .andExpect(status().isCreated());
    }

    private void verificarCuenta() throws Exception {
        mockMvc.perform(post("/api/v1/auth/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoVerificar(codigoVigente(CodigoVerificacion.VERIFICACION))))
                .andExpect(status().isOk());
    }

    private String codigoVigente(String tipo) {
        Usuario u = usuarioRepository.findByCorreo(CORREO).orElseThrow();
        return codigoRepository.findByUsuarioIdAndTipoAndUsadoEnIsNull(u.getId(), tipo)
                .orElseThrow().getCodigo();
    }

    private String cuerpoVerificar(String codigo) {
        return """
                {"correo":"%s","codigo":"%s"}
                """.formatted(CORREO, codigo);
    }
}
