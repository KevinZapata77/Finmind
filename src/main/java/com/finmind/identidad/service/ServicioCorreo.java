package com.finmind.identidad.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Envio de los codigos por correo.
 *
 * Cuando finmind.correo.habilitado es false, el codigo se escribe en el log
 * en lugar de enviarse. Eso permite demostrar el flujo completo sin depender
 * de un servidor de correo externo, que es la mitigacion registrada para el
 * riesgo RSK-07.
 *
 * LOS ENVIOS SALEN EN UN HILO APARTE (@Async)
 * Hablar con un servidor SMTP tarda segundos, y hacerlo dentro de la peticion
 * dejaba a la persona mirando un boton girando mientras su cuenta ya estaba
 * creada. El detalle de por que esto es seguro esta en ConfiguracionAsincrona.
 *
 * DOS CONSECUENCIAS QUE HAY QUE TENER PRESENTES
 * 1. Quien llama a estos metodos NO se entera de si el correo salio. Es
 *    intencional: nunca se entero, porque el try/catch de abajo ya se tragaba
 *    el fallo. La unica prueba de que un correo salio es el log.
 * 2. La anotacion solo hace efecto cuando el metodo se llama desde OTRO bean
 *    —Spring la aplica con un proxy—. Si algun dia se llamara a
 *    enviarCodigoVerificacion desde dentro de esta misma clase, volveria a ser
 *    sincrono sin avisar. Hoy el unico que llama es ServicioIdentidad.
 */
@Service
public class ServicioCorreo {

    private static final Logger log = LoggerFactory.getLogger(ServicioCorreo.class);

    private final JavaMailSender remitente;
    private final boolean habilitado;
    private final String de;

    private final String host;
    private final int puerto;
    private final boolean hayUsuario;
    private final boolean hayClave;

    public ServicioCorreo(JavaMailSender remitente,
                          @Value("${finmind.correo.habilitado:false}") boolean habilitado,
                          @Value("${finmind.correo.remitente}") String de,
                          @Value("${spring.mail.username:}") String cuentaSmtp,
                          @Value("${spring.mail.password:}") String claveSmtp,
                          @Value("${spring.mail.host:}") String host,
                          @Value("${spring.mail.port:0}") int puerto) {
        this.remitente = remitente;
        this.habilitado = habilitado;
        this.de = resolverRemitente(de, cuentaSmtp);
        this.host = host;
        this.puerto = puerto;
        this.hayUsuario = cuentaSmtp != null && !cuentaSmtp.isBlank();
        this.hayClave = claveSmtp != null && !claveSmtp.isBlank();
    }

    /**
     * Deja escrito en el log como quedo configurado el correo al arrancar.
     *
     * POR QUE HACE FALTA
     * Cuando un correo no llega hay cuatro sospechosos —el envio esta apagado,
     * falta el usuario, falta la clave, o el remitente esta mal— y desde afuera
     * los cuatro se ven igual: no pasa nada. Diagnosticarlo a ciegas es cambiar
     * cosas al azar y volver a probar, que es justo lo que se hizo tres veces
     * antes de escribir esto.
     *
     * Dos lineas en el arranque responden la pregunta de una vez, y quedan
     * disponibles en el panel del servidor sin tener que reproducir nada.
     *
     * NO SE REGISTRA NINGUN SECRETO
     * De la clave solo se dice si existe, nunca su valor. Un log es un archivo
     * que sobrevive al despliegue y que puede leer mas gente de la que uno cree.
     */
    @jakarta.annotation.PostConstruct
    void avisarComoQuedo() {
        if (!habilitado) {
            log.warn("CORREO APAGADO (finmind.correo.habilitado=false). Los codigos se "
                    + "escriben en este log en vez de enviarse. En produccion esto "
                    + "significa que nadie va a recibir nada: define MAIL_ENABLED=true.");
            return;
        }
        log.info("Correo ACTIVO. Servidor={}:{} remitente={} usuario={} clave={}",
                host.isBlank() ? "(sin definir)" : host, puerto,
                de,
                hayUsuario ? "definido" : "SIN DEFINIR",
                hayClave ? "definida" : "SIN DEFINIR");

        if (!hayUsuario || !hayClave) {
            log.error("El correo esta activo pero faltan credenciales SMTP. Cada envio va a "
                    + "fallar y el unico rastro sera una linea de error por mensaje. "
                    + "Revisa MAIL_USERNAME y MAIL_PASSWORD.");
        }

        /*
          DEF-032. El aviso que nos habria ahorrado media noche.

          Render bloquea el trafico saliente a los puertos 25, 465 y 587 en los
          servicios gratuitos desde el 26 de septiembre de 2025. No rechaza la
          conexion: descarta los paquetes. Asi que el sintoma no es un error de
          autenticacion ni de remitente —los dos primeros lugares donde uno
          mira—, sino un timeout seco al conectar:

            Couldn't connect to host, port: smtp.gmail.com, 587; timeout 5000

          Con credenciales correctas y configuracion correcta. El problema esta
          en la red, no en la aplicacion, y eso es justo lo que no se deduce
          mirando el codigo.

          El puerto 2525 no esta bloqueado, y lo ofrecen los relays de correo
          habituales. Cambiar MAIL_HOST, MAIL_PORT, MAIL_USERNAME y
          MAIL_PASSWORD alcanza: no hay una sola linea de codigo que tocar.
        */
        if (puerto == 25 || puerto == 465 || puerto == 587) {
            log.warn("El puerto {} suele estar bloqueado en los planes gratuitos de las "
                    + "plataformas de despliegue (Render lo hace desde septiembre de 2025). "
                    + "Si los envios fallan con 'Couldn't connect to host ... timeout', no es "
                    + "la configuracion: es la red. Usa un relay de correo por el puerto 2525.",
                    puerto);
        }
    }

    /**
     * DEF-027. Gmail solo deja enviar "desde" la cuenta autenticada.
     *
     * EL DEFECTO QUE ESTO CORRIGE
     * Los correos de verificacion dejaron de llegar, sin ningun error visible.
     * La causa era la direccion del remitente: si MAIL_FROM no esta definida en
     * el servidor, el valor por omision es no-responder@finmind.local, un
     * dominio que no existe. Gmail acepta la conexion, acepta la
     * autenticacion, y despues rechaza o descarta el mensaje porque el From no
     * corresponde a la cuenta con la que uno se autentico.
     *
     * Y el rechazo llegaba al log del servidor, no a la persona: desde afuera
     * el registro salia bien y el correo simplemente no aparecia nunca. Un
     * fallo silencioso, que es la peor clase.
     *
     * QUE HACE
     * Si el remitente configurado no es la propia cuenta SMTP, se usa la cuenta
     * SMTP y se avisa en el log. Preferir lo que funciona sobre lo que estaba
     * escrito es lo correcto aqui: la alternativa es no enviar nada.
     *
     * Cuando no hay cuenta SMTP —en desarrollo, con el correo apagado— se
     * respeta el valor configurado y no se avisa nada, porque ahi ese valor
     * nunca sale a la red.
     */
    private static String resolverRemitente(String configurado, String cuentaSmtp) {
        boolean haySmtp = cuentaSmtp != null && !cuentaSmtp.isBlank();
        if (!haySmtp) {
            return configurado;
        }
        /*
          OJO: solo se reemplaza el valor SIN CONFIGURAR, no cualquiera que no
          coincida.

          La primera version de este metodo usaba la cuenta SMTP siempre que
          fuera distinta del remitente, y con Gmail funcionaba porque ahi las
          dos cosas son la misma direccion. Pero en un relay de correo NO lo
          son: el usuario SMTP puede ser un identificador como
          8a1b2c001@smtp-brevo.com, mientras el remitente es la direccion real
          que uno verifico. Forzar el primero habria roto el envio justo al
          cambiar de proveedor, y por una "correccion" anterior.

          La regla buena es mas humilde: si el remitente sigue siendo el valor
          de relleno, no esta configurado y se usa la cuenta SMTP. Si alguien se
          tomo el trabajo de poner una direccion de verdad, se respeta.
        */
        boolean sinConfigurar = configurado == null || configurado.isBlank()
                || configurado.endsWith("@finmind.local");
        if (sinConfigurar) {
            log.warn("MAIL_FROM no esta configurado (vale '{}'), se envia desde la cuenta "
                    + "SMTP {}. Definilo para que el remitente sea el que vos elijas.",
                    configurado, cuentaSmtp);
            return cuentaSmtp;
        }
        return configurado;
    }

    @Async("ejecutorCorreo")
    public void enviarCodigoVerificacion(String destino, String nombre, String codigo, int minutos) {
        enviar(destino,
               "Verifica tu correo en FinMind",
               "Hola " + nombre + ",\n\n"
             + "Tu codigo de verificacion es: " + codigo + "\n\n"
             + "Vence en " + minutos + " minutos y solo se puede usar una vez.\n"
             + "Si no creaste esta cuenta, ignora este mensaje.\n\n"
             + "FinMind",
               codigo);
    }

    @Async("ejecutorCorreo")
    public void enviarCodigoRecuperacion(String destino, String nombre, String codigo, int minutos) {
        enviar(destino,
               "Recupera tu contrasena de FinMind",
               "Hola " + nombre + ",\n\n"
             + "Tu codigo para restablecer la contrasena es: " + codigo + "\n\n"
             + "Vence en " + minutos + " minutos y solo se puede usar una vez.\n"
             + "Si no solicitaste este cambio, ignora este mensaje: tu contrasena no cambia.\n\n"
             + "FinMind",
               codigo);
    }

    private void enviar(String destino, String asunto, String cuerpo, String codigo) {
        if (!habilitado) {
            // Modo desarrollo: el codigo queda a la vista de quien opera la aplicacion.
            // Nunca debe activarse en un ambiente con usuarios reales.
            log.warn("Correo deshabilitado. Codigo para {}: {}", destino, codigo);
            return;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            /*
              Con nombre visible, no la direccion pelada.
              Un mensaje que llega de "FinMind" y no de una direccion suelta se
              reconoce de un vistazo en la bandeja, y los filtros de correo
              tratan bastante peor a los remitentes sin nombre.

              DEF-033. Pero solo si no lo trae ya.
              La plantilla .env.example sugiere MAIL_FROM=FinMind <correo>, con
              el nombre incluido. Envolviendolo de nuevo salia
              "FinMind <FinMind <correo>>", que no es una direccion valida: el
              servidor la rechaza y el correo no sale. Un fallo que depende de
              como este escrita una variable de entorno es de los mas dificiles
              de ver, porque el codigo se lee bien.
            */
            mensaje.setFrom(de.contains("<") ? de : "FinMind <" + de + ">");
            mensaje.setTo(destino);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            remitente.send(mensaje);
            log.info("Codigo enviado a {}", destino);
        } catch (Exception ex) {
            /*
              Un fallo de correo no rompe el registro: el codigo ya quedo
              guardado y se puede pedir de nuevo.

              Pero SI tiene que quedar rastro util. Antes esto se registraba y
              nadie lo miraba, asi que el sintoma que llegaba era "no me llego
              el correo" sin ninguna pista de por que. El mensaje de abajo dice
              desde donde se intento enviar, que es justo el dato que faltaba
              cuando el remitente estaba mal (DEF-027).
            */
            log.error("No se pudo enviar el correo a {} desde {}: {}",
                    destino, de, ex.getMessage(), ex);
        }
    }
}
