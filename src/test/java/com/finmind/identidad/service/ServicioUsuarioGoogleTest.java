package com.finmind.identidad.service;

import com.finmind.identidad.service.ServicioUsuarioGoogle.CuentaGoogleException;
import com.finmind.identidad.service.ServicioUsuarioGoogle.Motivo;
import com.finmind.cuentas.service.ServicioCuentaInicial;
import com.finmind.usuarios.entity.Rol;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.RolRepository;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RN-033: cuando se vincula una cuenta de Google con una que ya existe.
 *
 * POR QUE ESTAS PRUEBAS SON DE LAS MAS IMPORTANTES DEL PROYECTO
 * Aqui se decide si una persona entra a SU cuenta o a la de otra. Un fallo en
 * el modulo de presupuestos muestra una cifra equivocada; un fallo aqui entrega
 * las finanzas de alguien a un desconocido, y lo hace en silencio, porque desde
 * afuera se ve exactamente igual que un acceso correcto.
 *
 * Son con Mockito y no de integracion a proposito: varias comprueban que el
 * repositorio NO se toco. Que no se haya guardado nada es justo lo que hay que
 * demostrar cuando se rechaza un acceso, y una prueba de integracion no
 * distingue entre "no guardo" y "guardo y luego fallo".
 *
 * EL ATAQUE QUE SE ESTA PROBANDO
 * Apropiacion previa de cuenta: alguien registra victima@gmail.com en FinMind
 * con una contrasena que el elige, y espera. Si al llegar la persona real por
 * Google la aplicacion fusionara las cuentas por el correo, la dejaria entrar
 * en la cuenta del atacante —que conserva la contrasena y mira todo—.
 *
 * Lo que lo impide es que esa cuenta usurpada nunca esta verificada, porque el
 * codigo llega al buzon real y no al atacante. Sobre esa diferencia se decide
 * todo: la cuenta verificada se VINCULA y conserva su contrasena (RN-033); la
 * que no lo esta pasa a Google y PIERDE la contrasena (RN-034).
 *
 * Ese borrado es el detalle que sostiene la seguridad del segundo caso, y por
 * eso tiene su propia prueba. Sin el, entregar la cuenta seria regalarsela al
 * atacante ya verificada.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Acceso con Google")
class ServicioUsuarioGoogleTest {

    private static final String CORREO = "kevin@gmail.com";
    private static final String GOOGLE_ID = "108374652910";

    @Mock private UsuarioRepository usuarios;
    @Mock private RolRepository roles;
    @Mock private ServicioCuentaInicial cuentaInicial;
    @InjectMocks private ServicioUsuarioGoogle servicio;

    // ------------------------------------------------------------- vinculacion

    @Nested
    @DisplayName("sobre una cuenta que ya existe")
    class CuentaExistente {

        @Test
        @DisplayName("la cuenta local verificada se vincula y conserva sus datos")
        void laCuentaVerificadaSeVincula() {
            Usuario local = local(CORREO, true);
            String hashOriginal = local.getContrasenaHash();
            when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.of(local));

            Usuario resultado = servicio.obtenerOCrear(perfil(CORREO));

            assertThat(resultado).isSameAs(local);
            assertThat(resultado.tieneGoogle()).isTrue();
            assertThat(resultado.getProveedorId()).isEqualTo(GOOGLE_ID);

            /*
              Lo que NO debe cambiar. Vincular agrega una forma de entrar; no es
              una excusa para reescribir la cuenta.

              La contrasena sobre todo: si se borrara, la persona perderia el
              acceso que ya tenia por haber usado un boton de conveniencia, y el
              dia que Google fallara se quedaria fuera sin remedio.
            */
            assertThat(resultado.getContrasenaHash())
                    .as("vincular no puede tocar la contrasena")
                    .isEqualTo(hashOriginal);
            assertThat(resultado.getNombre())
                    .as("el nombre de la cuenta manda sobre el de Google")
                    .isEqualTo("Kevin");
            assertThat(resultado.esLocal())
                    .as("la cuenta nacio LOCAL y eso es historico: no cambia")
                    .isTrue();

            // No se crea una segunda cuenta: se reusa la que habia.
            verify(usuarios, never()).save(any());
        }

        @Test
        @DisplayName("la cuenta local SIN verificar pasa a Google y pierde la contrasena")
        void laCuentaSinVerificarPasaAGoogle() {
            /*
              El escenario del atacante, y la prueba que sostiene RN-034.

              Antes esto se rechazaba, y el efecto real era encerrar al dueno
              legitimo: sin verificar no podia entrar por contrasena (RN-011)
              ni por Google, y para verificar necesitaba un correo que podia no
              llegarle. Tres puertas y las tres cerradas.

              Ahora la cuenta se entrega a quien probo el buzon. Lo que hay que
              demostrar aqui no es que entre —eso se ve—, sino que el hash que
              nadie comprobo DESAPARECE. Si sobreviviera, el atacante entraria
              despues con su contrasena a la cuenta de la victima, ya verificada
              y con datos: exactamente el ataque que se quiere impedir.
            */
            Usuario usurpada = local(CORREO, false);
            assertThat(usurpada.tieneContrasena()).isTrue();   // punto de partida

            when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.of(usurpada));

            Usuario resultado = servicio.obtenerOCrear(perfil(CORREO));

            assertThat(resultado).isSameAs(usurpada);
            assertThat(resultado.tieneGoogle()).isTrue();
            assertThat(resultado.estaVerificado())
                    .as("Google probo el buzon, asi que la cuenta queda verificada")
                    .isTrue();
            assertThat(resultado.tieneContrasena())
                    .as("la contrasena que nadie comprobo tiene que desaparecer")
                    .isFalse();

            // Se reusa la cuenta existente, no se crea una segunda.
            verify(usuarios, never()).save(any());
        }

        @Test
        @DisplayName("la cuenta que ya tenia Google se reutiliza")
        void laCuentaConGoogleSeReutiliza() {
            Usuario deGoogle = Usuario.deGoogle("Kevin", "Zapata", CORREO, rol(), GOOGLE_ID);
            when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.of(deGoogle));

            assertThat(servicio.obtenerOCrear(perfil(CORREO))).isSameAs(deGoogle);
            verify(usuarios, never()).save(any());
        }

        @Test
        @DisplayName("la cuenta desactivada no entra por Google")
        void laCuentaDesactivadaNoEntra() {
            /*
              Sin esto, desactivar a alguien solo le cerraria la puerta de la
              contrasena y el boton de Google seguiria abierto. Una expulsion a
              medias no es una expulsion.
            */
            Usuario suspendida = local(CORREO, true);
            suspendida.setActivo(false);
            when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.of(suspendida));

            assertThatThrownBy(() -> servicio.obtenerOCrear(perfil(CORREO)))
                    .isInstanceOf(CuentaGoogleException.class)
                    .extracting(e -> ((CuentaGoogleException) e).getMotivo())
                    .isEqualTo(Motivo.CUENTA_INACTIVA);
        }
    }

    // ------------------------------------------------ lo que afirma Google

    @Nested
    @DisplayName("sobre lo que afirma Google")
    class DatosDeGoogle {

        @Test
        @DisplayName("un correo que Google no verifico no sirve para nada")
        void elCorreoSinVerificarDeGoogleSeRechaza() {
            /*
              Pasa con cuentas de Workspace: el administrador del dominio puede
              crear alias que Google nunca comprobo. Si se aceptaran, cualquiera
              con un dominio propio podria fabricar un perfil que diga ser
              kevin@gmail.com y vincularse a esa cuenta.

              Se rechaza ANTES de consultar la base: no hace falta saber si la
              cuenta existe para saber que este correo no prueba nada. Y de paso
              no se filtra por tiempos de respuesta que ese correo este
              registrado.
            */
            Map<String, Object> atributos = atributosBase(CORREO);
            atributos.put("email_verified", false);

            assertThatThrownBy(() -> servicio.obtenerOCrear(perfilCon(atributos)))
                    .isInstanceOf(CuentaGoogleException.class)
                    .extracting(e -> ((CuentaGoogleException) e).getMotivo())
                    .isEqualTo(Motivo.GOOGLE_SIN_VERIFICAR);

            verify(usuarios, never()).findByCorreo(any());
        }

        @Test
        @DisplayName("si falta el atributo email_verified tampoco se confia")
        void sinElAtributoTampocoSeConfia() {
            // Ausente no es lo mismo que falso, pero se trata igual: lo que no
            // esta afirmado no esta probado.
            Map<String, Object> atributos = atributosBase(CORREO);
            atributos.remove("email_verified");

            assertThatThrownBy(() -> servicio.obtenerOCrear(perfilCon(atributos)))
                    .isInstanceOf(CuentaGoogleException.class);
        }

        @Test
        @DisplayName("un perfil sin correo se rechaza")
        void elPerfilSinCorreoSeRechaza() {
            Map<String, Object> atributos = atributosBase(CORREO);
            atributos.remove("email");

            assertThatThrownBy(() -> servicio.obtenerOCrear(perfilCon(atributos)))
                    .isInstanceOf(CuentaGoogleException.class)
                    .extracting(e -> ((CuentaGoogleException) e).getMotivo())
                    .isEqualTo(Motivo.SIN_CORREO);
        }
    }

    // ------------------------------------------------------ cuenta nueva

    @Nested
    @DisplayName("cuando no existe la cuenta")
    class CuentaNueva {

        @Test
        @DisplayName("se crea verificada y con los datos de Google")
        void seCreaVerificada() {
            when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.empty());
            when(roles.findByNombre(Rol.USUARIO)).thenReturn(Optional.of(rol()));
            when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

            Usuario creado = servicio.obtenerOCrear(perfil(CORREO));

            assertThat(creado.getNombre()).isEqualTo("Kevin");
            assertThat(creado.getApellido()).isEqualTo("Zapata");
            assertThat(creado.getCorreo()).isEqualTo(CORREO);
            assertThat(creado.tieneGoogle()).isTrue();
            // Nace verificada: Google ya comprobo el buzon y pedir un codigo
            // seria pedir dos veces la misma prueba.
            assertThat(creado.estaVerificado()).isTrue();
            assertThat(creado.tieneContrasena()).isFalse();
            assertThat(creado.estaActivo()).isTrue();
        }

        @Test
        @DisplayName("el nombre sale del correo cuando Google no lo manda")
        void elNombreSaleDelCorreo() {
            /*
              Antes estas cuentas quedaban llamandose "Usuario Google": se veia
              en el saludo del panel y en las iniciales del avatar, y delataba
              en la base cuales venian de Google. Ahora se usa el dato de la
              persona, aunque sea imperfecto.
            */
            Map<String, Object> atributos = atributosBase(CORREO);
            atributos.remove("given_name");
            atributos.remove("family_name");
            atributos.remove("name");

            when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.empty());
            when(roles.findByNombre(Rol.USUARIO)).thenReturn(Optional.of(rol()));
            when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

            Usuario creado = servicio.obtenerOCrear(perfilCon(atributos));

            assertThat(creado.getNombre()).isEqualTo("Kevin");
            assertThat(creado.getNombre()).doesNotContain("Usuario");
            assertThat(creado.getApellido()).isNotBlank();
        }

        @Test
        @DisplayName("DEF-030: la cuenta nueva recibe su cuenta de efectivo inicial")
        void laCuentaNuevaRecibeSuCuentaDeEfectivo() {
            /*
              Faltaba, y el sintoma era desconcertante: quien entraba con Google
              llegaba a una aplicacion sin ninguna cuenta, y al anotar su primer
              movimiento se encontraba un desplegable vacio sin explicacion.

              El registro con correo y contrasena si la creaba. La logica estaba
              escrita en un solo camino y nadie la repitio en el otro, que es
              justo lo que pasa cuando algo importante vive suelto en vez de en
              su propio servicio.
            */
            when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.empty());
            when(roles.findByNombre(Rol.USUARIO)).thenReturn(Optional.of(rol()));
            when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

            Usuario creado = servicio.obtenerOCrear(perfil(CORREO));

            verify(cuentaInicial).crearSiNoTiene(creado);
        }

        @Test
        @DisplayName("el correo se normaliza a minusculas")
        void elCorreoSeNormaliza() {
            // Google puede devolver "Kevin@Gmail.com". Sin normalizar, se crearia
            // una cuenta nueva junto a la que ya existe en minusculas, y la
            // persona veria su aplicacion vacia sin entender por que.
            when(usuarios.findByCorreo(CORREO)).thenReturn(Optional.empty());
            when(roles.findByNombre(Rol.USUARIO)).thenReturn(Optional.of(rol()));
            when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

            Usuario creado = servicio.obtenerOCrear(perfil("  Kevin@GMAIL.com  "));

            assertThat(creado.getCorreo()).isEqualTo(CORREO);
        }
    }

    // ------------------------------------------------------------- utilidades

    private static Rol rol() {
        return new Rol(Rol.USUARIO, "Usuario estandar");
    }

    private static Usuario local(String correo, boolean verificado) {
        Usuario u = new Usuario("Kevin", "Zapata", correo, "$2a$10$hashDePrueba", rol());
        if (verificado) u.marcarCorreoVerificado();
        return u;
    }

    private static Map<String, Object> atributosBase(String correo) {
        Map<String, Object> m = new HashMap<>();
        m.put("sub", GOOGLE_ID);
        m.put("email", correo);
        m.put("email_verified", true);
        m.put("given_name", "Kevin");
        m.put("family_name", "Zapata");
        m.put("name", "Kevin Zapata");
        return m;
    }

    private static OAuth2User perfil(String correo) {
        return perfilCon(atributosBase(correo));
    }

    private static OAuth2User perfilCon(Map<String, Object> atributos) {
        // 'sub' como clave del nombre es lo que usa Spring Security con Google.
        return new DefaultOAuth2User(List.of(), atributos, "sub");
    }
}
