package com.finmind.identidad.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Verificacion del CAPTCHA en el servidor.
 *
 * El cliente muestra el widget y obtiene un token. Ese token llega al
 * registro y AQUI se comprueba contra el proveedor con la clave secreta.
 *
 * Comprobarlo solo en el navegador no sirve de nada: cualquiera puede
 * llamar al endpoint directamente. La validacion tiene que ocurrir en el
 * servidor, igual que cualquier otra regla (API-02).
 *
 * Funciona con Cloudflare Turnstile y con Google reCAPTCHA: los dos
 * reciben un formulario con "secret" y "response", y responden un JSON
 * con el campo "success". Solo cambia la URL.
 */
@Service
public class ServicioCaptcha {

    private static final Logger log = LoggerFactory.getLogger(ServicioCaptcha.class);

    private final RestClient cliente;
    private final boolean habilitado;
    private final String secreto;
    private final String urlVerificacion;

    public ServicioCaptcha(RestClient.Builder builder,
                           @Value("${finmind.captcha.habilitado:false}") boolean habilitado,
                           @Value("${finmind.captcha.secreto:}") String secreto,
                           @Value("${finmind.captcha.url-verificacion}") String urlVerificacion) {
        this.cliente = builder.build();
        this.habilitado = habilitado;
        this.secreto = secreto;
        this.urlVerificacion = urlVerificacion;
    }

    /**
     * @throws CaptchaInvalidoException si el token falta, es falso o el
     *         proveedor lo rechaza.
     */
    public void verificar(String token) {
        if (!habilitado) {
            // Modo desarrollo: permite probar el registro sin claves del proveedor.
            log.debug("CAPTCHA deshabilitado: se omite la verificacion");
            return;
        }

        if (token == null || token.isBlank()) {
            throw new CaptchaInvalidoException("Confirma que no eres un robot para continuar.");
        }

        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("secret", secreto);
        formulario.add("response", token);

        try {
            JsonNode respuesta = cliente.post()
                    .uri(urlVerificacion)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formulario)
                    .retrieve()
                    .body(JsonNode.class);

            boolean valido = respuesta != null
                    && respuesta.path("success").asBoolean(false);

            if (!valido) {
                log.warn("CAPTCHA rechazado por el proveedor: {}",
                        respuesta == null ? "sin respuesta" : respuesta.path("error-codes"));
                throw new CaptchaInvalidoException(
                        "No pudimos confirmar que eres una persona. Intenta de nuevo.");
            }
        } catch (CaptchaInvalidoException ex) {
            throw ex;
        } catch (Exception ex) {
            // Si el proveedor no responde, se rechaza el registro.
            // Dejar pasar ante un fallo convertiria el control en decorativo.
            log.error("No se pudo consultar al proveedor de CAPTCHA", ex);
            throw new CaptchaInvalidoException(
                    "No pudimos validar la verificacion. Intenta de nuevo en un momento.");
        }
    }

    /** 400: el registro no continua. */
    public static class CaptchaInvalidoException extends RuntimeException {
        public CaptchaInvalidoException(String mensaje) { super(mensaje); }
    }
}
