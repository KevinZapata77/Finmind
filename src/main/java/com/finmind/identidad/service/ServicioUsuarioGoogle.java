package com.finmind.identidad.service;

import com.finmind.cuentas.service.ServicioCuentaInicial;
import com.finmind.usuarios.entity.Rol;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.RolRepository;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Traduce el perfil que devuelve Google a un usuario de FinMind.
 *
 * LA REGLA DE ORO (RN-033)
 * Un correo es una persona. Si alguien creo su cuenta con contrasena y despues
 * entra con Google usando ese mismo correo, es el mismo dueno del mismo buzon:
 * debe caer en su cuenta de siempre, con sus movimientos y sus presupuestos,
 * no en una cuenta nueva y vacia ni contra un error.
 *
 * POR QUE ESTO NO ES TAN OBVIO COMO PARECE
 * Fusionar cuentas por el correo, a secas, es un agujero con nombre propio:
 * apropiacion previa de cuenta. Funciona asi:
 *
 *   1. El atacante registra victima@gmail.com en FinMind con una contrasena
 *      que el elige. No tiene acceso a ese buzon, pero nadie se lo impide.
 *   2. Tiempo despues la persona real llega y pulsa "Entrar con Google".
 *   3. Si la aplicacion fusionara por correo sin mas, la dejaria entrar a la
 *      cuenta que creo el atacante — y el atacante entra cuando quiera, porque
 *      sabe la contrasena. Se queda mirando las finanzas de la victima.
 *
 * COMO SE CIERRA: MIRANDO QUIEN PROBO QUE
 * La clave esta en si la cuenta local fue verificada, porque verificarla exige
 * leer un codigo que llego a ese buzon. Hay dos desenlaces y ninguno rechaza:
 *
 *   a) CUENTA VERIFICADA -> se VINCULA (RN-033). Las dos partes probaron
 *      controlar el mismo buzon por caminos que no se tocan: el codigo de
 *      FinMind y el email_verified de Google. Es la misma persona, y la cuenta
 *      conserva absolutamente todo, contrasena incluida.
 *
 *   b) CUENTA SIN VERIFICAR -> Google SE QUEDA con ella (RN-034). Esa cuenta no
 *      probo nada: pudo haberla creado cualquiera con el correo ajeno. Google
 *      si acaba de probar el control del buzon, asi que recibe la cuenta y se
 *      borra el hash que nadie comprobo. El atacante del paso 1 no solo no gana
 *      nada: pierde la contrasena que habia puesto.
 *
 * Y AL CONTRARIO: UNA CUENTA VERIFICADA NO REGALA SU CONTRASENA
 * Vincular agrega una forma de entrar; no revela ni cambia la que ya habia.
 *
 * Los mensajes no se escriben aqui: se devuelve un CODIGO corto y el frontend
 * decide el texto. Ver ManejadorExitoGoogle para el por que.
 */
@Service
public class ServicioUsuarioGoogle {

    private static final Logger log = LoggerFactory.getLogger(ServicioUsuarioGoogle.class);

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final ServicioCuentaInicial cuentaInicial;

    public ServicioUsuarioGoogle(UsuarioRepository usuarios, RolRepository roles,
                                 ServicioCuentaInicial cuentaInicial) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.cuentaInicial = cuentaInicial;
    }

    @Transactional
    public Usuario obtenerOCrear(OAuth2User perfil) {
        String correo = valor(perfil, "email");
        if (correo == null || correo.isBlank()) {
            throw new CuentaGoogleException(Motivo.SIN_CORREO);
        }
        correo = correo.trim().toLowerCase();

        /*
          Google no siempre garantiza el correo.

          En las cuentas de Workspace el administrador del dominio puede crear
          alias y direcciones que Google nunca comprobo. Si email_verified viene
          en false, ese correo NO es una prueba de nada y no puede servir ni
          para vincular ni para crear una cuenta: alguien podria hacerse pasar
          por el dueno de una direccion ajena.

          El atributo se lee de forma defensiva porque puede llegar como
          booleano o como cadena segun el proveedor.
        */
        if (!correoVerificadoPorGoogle(perfil)) {
            log.warn("Google entrego un correo sin verificar: {}", correo);
            throw new CuentaGoogleException(Motivo.GOOGLE_SIN_VERIFICAR);
        }

        String googleId = valor(perfil, "sub");
        var existente = usuarios.findByCorreo(correo);

        if (existente.isPresent()) {
            return usarCuentaExistente(existente.get(), googleId);
        }
        return crear(perfil, correo, googleId);
    }

    /**
     * La cuenta ya existe. Tres caminos, y ninguno deja a nadie afuera.
     */
    private Usuario usarCuentaExistente(Usuario u, String googleId) {
        if (!u.estaActivo()) {
            throw new CuentaGoogleException(Motivo.CUENTA_INACTIVA);
        }

        // Camino 1: ya tenia Google. Es el caso normal de quien vuelve.
        if (u.tieneGoogle()) {
            u.registrarAcceso();
            return u;
        }

        /*
          Camino 2: cuenta local SIN verificar. Google se queda con ella (RN-034).

          DEF-029. ANTES ESTO SE RECHAZABA, Y DEJABA A LA PERSONA ENCERRADA.
          El razonamiento era defendible pero el resultado no: quien se
          registraba, no alcanzaba a escribir el codigo y volvia por Google se
          quedaba sin ninguna puerta. Por Google, rechazado. Por contrasena,
          rechazado tambien, porque sin verificar no se inicia sesion (RN-011).
          Y para verificar hacia falta un correo que, si no llegaba, cerraba el
          circulo. Tres caminos y los tres tapiados.

          Un candado que deja afuera al dueno no esta protegiendo nada.

          La salida no es aflojar la regla, es mirar quien probo que. Esa cuenta
          sin verificar no probo nada: cualquiera pudo escribir el correo ajeno.
          Google, en cambio, acaba de probar que quien esta del otro lado
          controla el buzon. Entre una credencial probada y una sin probar sobre
          el mismo correo, gana la probada, y la otra se borra.

          El atacante del escenario de arriba no gana nada con esto: su cuenta
          usurpada nunca pudo iniciar sesion, asi que esta vacia, y al entregarla
          se le borra la contrasena que habia puesto. Queda peor que antes.
        */
        if (!u.estaVerificado()) {
            u.tomarPosesionConGoogle(googleId);
            u.registrarAcceso();
            log.info("La cuenta sin verificar de {} pasa a Google: se borro la contrasena "
                    + "que nadie habia comprobado", u.getCorreo());
            return u;
        }

        // Camino 3: cuenta local verificada. Se vincula y conserva todo (RN-033).
        u.vincularGoogle(googleId);
        u.registrarAcceso();
        log.info("Cuenta de {} vinculada con Google", u.getCorreo());
        return u;
    }

    private Usuario crear(OAuth2User perfil, String correo, String googleId) {
        Rol rolUsuario = roles.findByNombre(Rol.USUARIO)
                .orElseThrow(() -> new IllegalStateException(
                        "El rol " + Rol.USUARIO + " no existe. Verifica que la migracion V1 se aplico."));

        Usuario nuevo = Usuario.deGoogle(
                nombreDe(perfil, correo), apellidoDe(perfil), correo, rolUsuario, googleId);
        nuevo.registrarAcceso();
        Usuario guardado = usuarios.save(nuevo);

        /*
          DEF-030. Esta linea faltaba, y era la causa de que quien entraba con
          Google llegara a una aplicacion sin ninguna cuenta: al intentar anotar
          su primer movimiento se encontraba con un desplegable vacio y sin
          ninguna explicacion.

          El registro con correo y contrasena si la creaba. Cuando se agrego
          Google, la logica quedo escrita en un solo camino y nadie la repitio
          en el otro. Por eso ahora vive en su propio servicio: para que el
          tercer camino de registro que aparezca no vuelva a olvidarla.
        */
        cuentaInicial.crearSiNoTiene(guardado);

        log.info("Cuenta nueva creada desde Google para {}", correo);
        return guardado;
    }

    /*
      NOMBRE Y APELLIDO: QUE LA FILA QUEDE COMO LA DE CUALQUIER OTRO USUARIO

      Antes, si Google no mandaba given_name/family_name, la cuenta quedaba
      llamandose "Usuario Google". Aparecia asi en el saludo del panel y en las
      iniciales del avatar, y en la base se distinguia a simple vista cuales
      venian de Google, que no deberia notarse.

      Ahora hay tres intentos en orden de calidad, y el ultimo siempre da algo
      razonable: given_name/family_name, luego partir 'name', y si tampoco, la
      parte del correo antes de la arroba con la inicial en mayuscula. De
      "kevinzapatajega@gmail.com" sale "Kevinzapatajega", que no es perfecto
      pero es SU dato, no una etiqueta puesta por la aplicacion. Y como el
      nombre es editable, se puede corregir.
    */
    private static String nombreDe(OAuth2User perfil, String correo) {
        String dado = valor(perfil, "given_name");
        if (noVacio(dado)) return recortar(dado);

        String completo = valor(perfil, "name");
        if (noVacio(completo)) return recortar(completo.trim().split("\\s+")[0]);

        String antesDeLaArroba = correo.split("@")[0].replaceAll("[^\\p{L}\\p{N}]", "");
        if (antesDeLaArroba.isBlank()) return "Usuario";
        return recortar(Character.toUpperCase(antesDeLaArroba.charAt(0)) + antesDeLaArroba.substring(1));
    }

    private static String apellidoDe(OAuth2User perfil) {
        String familia = valor(perfil, "family_name");
        if (noVacio(familia)) return recortar(familia);

        String completo = valor(perfil, "name");
        if (noVacio(completo)) {
            String[] partes = completo.trim().split("\\s+");
            if (partes.length > 1) return recortar(partes[partes.length - 1]);
        }
        /*
          El apellido es NOT NULL en la base y Google no siempre lo manda —las
          cuentas personales antiguas suelen traer solo un nombre—. Un punto es
          menos ruidoso que la palabra "Google" y se nota que hay que
          completarlo.
        */
        return ".";
    }

    private static boolean correoVerificadoPorGoogle(OAuth2User perfil) {
        Object v = perfil.getAttributes().get("email_verified");
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(v.toString());
    }

    private static String valor(OAuth2User perfil, String clave) {
        Object v = perfil.getAttributes().get(clave);
        return v == null ? null : v.toString();
    }

    private static boolean noVacio(String s) {
        return s != null && !s.isBlank();
    }

    /** La base admite 80 caracteres en nombre y apellido. */
    private static String recortar(String texto) {
        String limpio = texto.trim();
        return limpio.length() > 80 ? limpio.substring(0, 80) : limpio;
    }

    /**
     * Por que codigos y no frases.
     *
     * El motivo viaja hasta el navegador en la URL de retorno. Una frase en
     * castellano ahi dentro sale codificada —'%20ya%20tiene%20una%20cuenta'— y
     * queda a la vista en la barra de direcciones: se lee como si la aplicacion
     * hubiera reventado. Ademas ata el texto que ve el usuario a una clase de
     * servicio del backend, que es el ultimo lugar donde deberia editarse.
     *
     * Con un codigo corto la URL queda limpia y el texto vive en el frontend,
     * junto al resto de los mensajes.
     */
    public enum Motivo {
        SIN_CORREO("google_sin_correo"),
        GOOGLE_SIN_VERIFICAR("google_correo_sin_verificar"),
        CUENTA_INACTIVA("cuenta_inactiva");

        private final String codigo;

        Motivo(String codigo) { this.codigo = codigo; }

        public String codigo() { return codigo; }
    }

    public static class CuentaGoogleException extends RuntimeException {
        private final Motivo motivo;

        public CuentaGoogleException(Motivo motivo) {
            super(motivo.codigo());
            this.motivo = motivo;
        }

        public Motivo getMotivo() { return motivo; }
    }
}
