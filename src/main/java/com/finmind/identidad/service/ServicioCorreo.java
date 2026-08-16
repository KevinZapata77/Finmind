package com.finmind.identidad.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envio de los codigos por correo.
 *
 * Cuando finmind.correo.habilitado es false, el codigo se escribe en el log
 * en lugar de enviarse. Eso permite demostrar el flujo completo sin depender
 * de un servidor de correo externo, que es la mitigacion registrada para el
 * riesgo RSK-07.
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
