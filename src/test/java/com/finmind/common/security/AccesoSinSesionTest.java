package com.finmind.common.security;

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
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RN-015: sin sesion activa no se accede a ningun recurso de la aplicacion.
 *
 * Esconder el enlace en el navegador no es seguridad: cualquiera puede llamar
 * a la API directamente. Estas pruebas atacan la API sin pasar por la interfaz,
 * que es justo lo que haria alguien que quiera entrar sin permiso.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Acceso sin sesion")
class AccesoSinSesionTest {

    private static final String CORREO = "sin.sesion@finmind.test";
    private static final String CONTRASENA = "ClaveSegura123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
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

    // --------------------------------------------- Nadie entra sin token

    @Test
    @DisplayName("Sin token no se obtiene el perfil")
    void sinTokenNoHayPerfil() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token inventado se rechaza")
    void tokenInventadoRechazado() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer esto.no.es.un.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token valido sin el prefijo Bearer se rechaza")
    void tokenSinPrefijoRechazado() throws Exception {
        String token = registrarYVerificar();
        mockMvc.perform(get("/api/v1/usuarios/me").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un endpoint no listado queda protegido por defecto")
    void endpointNoListadoQuedaProtegido() throws Exception {
        // Si alguien agrega un controlador y olvida asegurarlo, este caso lo delata:
        // la configuracion niega por defecto en lugar de permitir por defecto.
        mockMvc.perform(get("/api/v1/cuentas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/transacciones")).andExpect(status().isUnauthorized());
    }

    // --------------------------------------------- La cuenta nace cerrada

    @Test
    @DisplayName("El registro no entrega token")
    void registroNoEntregaToken() throws Exception {
        registrarSinVerificar()
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("Una cuenta sin verificar no puede iniciar sesion")
    void cuentaSinVerificarNoEntra() throws Exception {
        registrarSinVerificar();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"%s","contrasena":"%s"}
                                """.formatted(CORREO, CONTRASENA)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tras verificar el correo si se entra al perfil")
    void conTokenValidoSiEntra() throws Exception {
        String token = registrarYVerificar();
        mockMvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(CORREO));
    }

    // --------------------------------------------- Lo publico, solo lo justo

    @Test
    @DisplayName("El chequeo de disponibilidad si es publico")
    void saludEsPublica() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("El resto de actuator no queda expuesto")
    void actuatorNoExponeMasDeLaCuenta() throws Exception {
        // Exponer /actuator/env filtraria variables de entorno, incluidas credenciales.
        mockMvc.perform(get("/actuator/env")).andExpect(result -> {
            if (result.getResponse().getStatus() == 200) {
                throw new AssertionError("/actuator/env quedo expuesto sin autenticacion");
            }
        });
    }

    // --------------------------------------------- apoyo

    private ResultActions registrarSinVerificar() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Ana","apellido":"Prueba","correo":"%s","contrasena":"%s"}
                        """.formatted(CORREO, CONTRASENA)));
    }

    private String registrarYVerificar() throws Exception {
        registrarSinVerificar().andExpect(status().isCreated());
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
}
