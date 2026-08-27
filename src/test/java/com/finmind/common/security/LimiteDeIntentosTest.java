package com.finmind.common.security;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Limite de intentos en las operaciones sensibles (SEG-03, SEG-04, SEG-05).
 *
 * Tres huecos que tenia la API y que estas pruebas dejan cerrados de forma
 * detectable:
 *
 *   SEG-03  el inicio de sesion no tenia tope de intentos
 *   SEG-04  el codigo bloqueaba a los 5 intentos, pero se podia pedir un codigo
 *           nuevo sin freno, lo que dejaba el tope sin efecto
 *   SEG-05  reenviar y recuperar no tenian tope, asi que se podia llenar de
 *           correos la bandeja de cualquiera
 *
 * Los limites por omision que se prueban aqui: 5 intentos de login por correo,
 * 20 por IP, y un envio de codigo cada 60 segundos con maximo 5 por hora.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Limite de intentos")
class LimiteDeIntentosTest {

    private static final String CORREO = "limite.prueba@finmind.test";
    private static final String OTRO_CORREO = "otro.limite@finmind.test";
    private static final String CONTRASENA = "ClaveSegura123";
    private static final String CLAVE_MALA = "ClaveEquivocada999";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CodigoVerificacionRepository codigoRepository;
    @Autowired private LimpiadorDeDatos limpiador;
    @Autowired private LimitadorDeIntentos limitador;

    @BeforeEach
    void prepararDatos() {
        // limpiar() reinicia tambien el limitador: sin eso, la primera prueba
        // que agota el tope dejaria bloqueadas a las siguientes.
        limpiador.limpiar();
    }

    // ============================================================== SEG-03

    @Test
    @DisplayName("SEG-03: tras 5 intentos fallidos el login responde 429 con Retry-After")
    void loginSeBloqueaTrasCincoFallos() throws Exception {
        registrarYVerificar(CORREO);

        // Los primeros cinco fallan por credenciales: 401, como siempre.
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoLogin(CORREO, CLAVE_MALA)))
                    .andExpect(status().isUnauthorized());
        }

        // El sexto ya no llega a comprobar la contrasena.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(CORREO, CLAVE_MALA)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    @DisplayName("SEG-03: con el limite alcanzado, la contrasena correcta tampoco entra")
    void elBloqueoNoSeSaltaConLaClaveBuena() throws Exception {
        registrarYVerificar(CORREO);

        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoLogin(CORREO, CLAVE_MALA)))
                    .andExpect(status().isUnauthorized());
        }

        // Si el bloqueo se pudiera saltar acertando, no serviria de nada:
        // quien prueba contrasenas al azar acabaria pasando igual.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(CORREO, CONTRASENA)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("SEG-03: entrar bien borra los fallos anteriores")
    void elExitoLimpiaElContador() throws Exception {
        registrarYVerificar(CORREO);

        // Dos equivocaciones y despues la buena: es lo que le pasa a cualquiera.
        for (int i = 1; i <= 2; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoLogin(CORREO, CLAVE_MALA)))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(CORREO, CONTRASENA)))
                .andExpect(status().isOk());

        // El contador quedo en cero, asi que vuelve a tener cinco margenes.
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoLogin(CORREO, CLAVE_MALA)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("SEG-03: bloquear una cuenta no bloquea a las demas")
    void elLimitePorCorreoNoAfectaAOtraCuenta() throws Exception {
        registrarYVerificar(CORREO);
        registrarYVerificar(OTRO_CORREO);

        for (int i = 1; i <= 6; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoLogin(CORREO, CLAVE_MALA)));
        }

        // Si el limite fuera global, un atacante bloquearia a todo el mundo
        // fallando a proposito. El tope por IP existe aparte y es mas alto.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(OTRO_CORREO, CONTRASENA)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SEG-03: el 429 no revela si la cuenta existe")
    void elBloqueoNoDelataCuentasInexistentes() throws Exception {
        String inexistente = "no.existe.para.nada@finmind.test";

        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoLogin(inexistente, CLAVE_MALA)))
                    .andExpect(status().isUnauthorized());
        }

        // Un correo que no existe se bloquea igual que uno que si. Si solo se
        // limitaran las cuentas reales, el 429 seria un delator perfecto.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(inexistente, CLAVE_MALA)))
                .andExpect(status().isTooManyRequests());
    }

    // ======================================================= SEG-04 y SEG-05

    @Test
    @DisplayName("SEG-05: dos reenvios seguidos no se admiten")
    void elReenvioRespetaElIntervaloMinimo() throws Exception {
        registrarSinVerificar(CORREO);

        // 202 y no 200: el envio es asincrono desde el punto de vista del cliente,
        // que es lo que estos endpoints ya devolvian antes de este cambio.
        mockMvc.perform(post("/api/v1/auth/reenviar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCorreo(CORREO)))
                .andExpect(status().isAccepted());

        // Antes esto se podia repetir en bucle: llenaba de correos la bandeja
        // de la victima y agotaba la cuota del proveedor.
        mockMvc.perform(post("/api/v1/auth/reenviar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCorreo(CORREO)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("SEG-04: el reenvio limitado es lo que da sentido al tope de 5 intentos")
    void noSePuedenPedirCodigosSinFreno() throws Exception {
        registrarSinVerificar(CORREO);
        Usuario usuario = usuarioRepository.findByCorreo(CORREO).orElseThrow();

        String primero = codigoRepository
                .findByUsuarioIdAndTipoAndUsadoEnIsNull(usuario.getId(),
                        CodigoVerificacion.VERIFICACION)
                .orElseThrow().getCodigo();

        // Se gastan los cinco intentos del codigo vigente.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/verificar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoVerificar(CORREO, "000000")));
        }

        // El dueno legitimo si puede pedir otro codigo: el primero pasa.
        mockMvc.perform(post("/api/v1/auth/reenviar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCorreo(CORREO)))
                .andExpect(status().isAccepted());

        // Lo que ya NO se puede es encadenarlos. Antes bastaba repetir esto para
        // tener cinco intentos mas cada vez, y asi sin fin: el tope de intentos
        // no significaba nada. Con el freno quedan 5 codigos por hora por 5
        // intentos, o sea 25 combinaciones de un millon posibles.
        mockMvc.perform(post("/api/v1/auth/reenviar-codigo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCorreo(CORREO)))
                .andExpect(status().isTooManyRequests());

        // AME-07: emitir uno nuevo invalida el anterior, asi que en ningun momento
        // hay dos codigos vivos a la vez. No se comparan los dos codigos entre si
        // porque son aleatorios y podrian coincidir: se cuenta cuantos quedan sin
        // usar, que es una comprobacion que nunca falla por azar.
        long vivos = codigoRepository.findAll().stream()
                .filter(c -> c.getUsuario().getId().equals(usuario.getId()))
                .filter(c -> CodigoVerificacion.VERIFICACION.equals(c.getTipo()))
                .filter(c -> c.getUsadoEn() == null)
                .count();
        assertThat(vivos).isEqualTo(1);
        assertThat(primero).hasSize(6);
    }

    @Test
    @DisplayName("SEG-05: la recuperacion tambien esta limitada y sigue sin delatar cuentas")
    void laRecuperacionRespetaElLimiteYLaRespuestaUniforme() throws Exception {
        String inexistente = "tampoco.existe@finmind.test";

        // RN-014: la primera responde 202 aunque el correo no exista. Ese 202
        // es exactamente el mismo que recibe una cuenta que si existe.
        mockMvc.perform(post("/api/v1/auth/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCorreo(inexistente)))
                .andExpect(status().isAccepted());

        // Y la segunda seguida da 429, exista o no. El limite se aplica ANTES de
        // buscar al usuario: si se aplicara despues, la diferencia entre 429 y
        // 200 diria cuales cuentas estan registradas.
        mockMvc.perform(post("/api/v1/auth/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCorreo(inexistente)))
                .andExpect(status().isTooManyRequests());
    }

    // ================================================== el limitador en si

    @Test
    @DisplayName("el limitador cuenta por clave y las claves no se pisan")
    void cadaClaveLlevaSuPropiaCuenta() {
        limitador.reiniciar();
        assertThat(limitador.clavesActivas()).isZero();

        limitador.comprobar("prueba:a", 2, java.time.Duration.ofMinutes(1), "tope");
        limitador.comprobar("prueba:a", 2, java.time.Duration.ofMinutes(1), "tope");
        limitador.comprobar("prueba:b", 2, java.time.Duration.ofMinutes(1), "tope");

        assertThat(limitador.clavesActivas()).isEqualTo(2);

        // La tercera de "a" se pasa del tope; "b" sigue con margen.
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        limitador.comprobar("prueba:a", 2, java.time.Duration.ofMinutes(1), "tope"))
                .isInstanceOf(com.finmind.common.exception.DemasiadosIntentosException.class);

        limitador.comprobar("prueba:b", 2, java.time.Duration.ofMinutes(1), "tope");
    }

    @Test
    @DisplayName("olvidar() borra la cuenta de una sola clave")
    void olvidarLimpiaSoloEsaClave() {
        limitador.reiniciar();
        limitador.comprobar("prueba:x", 1, java.time.Duration.ofMinutes(1), "tope");
        limitador.comprobar("prueba:y", 1, java.time.Duration.ofMinutes(1), "tope");

        limitador.olvidar("prueba:x");

        // x volvio a cero y admite otra; y ya estaba en su tope.
        limitador.comprobar("prueba:x", 1, java.time.Duration.ofMinutes(1), "tope");
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        limitador.comprobar("prueba:y", 1, java.time.Duration.ofMinutes(1), "tope"))
                .isInstanceOf(com.finmind.common.exception.DemasiadosIntentosException.class);
    }

    // ------------------------------------------------------------- apoyo

    private void registrarSinVerificar(String correo) throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(correo)))
                .andExpect(status().isCreated());
    }

    private void registrarYVerificar(String correo) throws Exception {
        registrarSinVerificar(correo);
        Usuario creado = usuarioRepository.findByCorreo(correo).orElseThrow();
        String codigo = codigoRepository
                .findByUsuarioIdAndTipoAndUsadoEnIsNull(creado.getId(),
                        CodigoVerificacion.VERIFICACION)
                .orElseThrow().getCodigo();

        mockMvc.perform(post("/api/v1/auth/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoVerificar(correo, codigo)))
                .andExpect(status().isOk());
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

    private String cuerpoCorreo(String correo) {
        return """
                {"correo":"%s"}
                """.formatted(correo);
    }

    private String cuerpoVerificar(String correo, String codigo) {
        return """
                {"correo":"%s","codigo":"%s"}
                """.formatted(correo, codigo);
    }
}
