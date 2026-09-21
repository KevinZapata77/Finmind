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

    public ServicioCorreo(JavaMailSender remitente,
                          @Value("${finmind.correo.habilitado:false}") boolean habilitado,
                          @Value("${finmind.correo.remitente}") String de) {
        this.remitente = remitente;
        this.habilitado = habilitado;
        this.de = de;
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
            mensaje.setFrom(de);
            mensaje.setTo(destino);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            remitente.send(mensaje);
            log.info("Codigo enviado a {}", destino);
        } catch (Exception ex) {
            // Un fallo de correo no debe romper el registro del usuario.
            // El codigo ya quedo persistido y se puede solicitar de nuevo.
            log.error("No se pudo enviar el correo a {}", destino, ex);
        }
    }
}
