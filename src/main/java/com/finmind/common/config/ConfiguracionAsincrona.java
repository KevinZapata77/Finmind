package com.finmind.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Hilos para el trabajo que no debe hacer esperar a quien esta en la pantalla.
 *
 * EL PROBLEMA QUE RESUELVE
 * El envio del codigo de verificacion se hacia dentro de la peticion HTTP. La
 * persona pulsaba "Crear cuenta" y el navegador quedaba esperando a que el
 * servidor terminara de hablar con Gmail: abrir el socket, negociar TLS,
 * autenticarse y entregar el mensaje. Entre dos y cinco segundos en los que la
 * cuenta YA estaba creada y el codigo YA estaba guardado, pero nadie lo sabia
 * todavia porque la respuesta no salia.
 *
 * Lo grave no es la espera en si, es que la espera depende de un tercero. Si
 * Gmail esta lento, la aplicacion parece lenta; si Gmail no contesta, la
 * aplicacion se queda colgada hasta que vence el tiempo de espera (5 s de
 * conexion + 5 s de lectura + 5 s de escritura, en el peor caso 15 s), y
 * mientras tanto ese hilo de Tomcat no atiende a nadie mas.
 *
 * Con esto, el envio se entrega a un hilo aparte y la respuesta sale de
 * inmediato. El correo sigue su camino por detras.
 *
 * POR QUE SE PUEDE HACER SIN PERDER NADA
 * Porque el correo no es la fuente de la verdad. El codigo se guarda en la
 * base ANTES de intentar el envio, y la pantalla siguiente ("escribe el codigo
 * que te llego") no depende de que el mensaje haya salido: existe el boton de
 * reenviar precisamente para eso. Un fallo de correo ya se tragaba y se
 * registraba en el log; ahora se traga en otro hilo, que es lo mismo desde
 * afuera.
 *
 * POR QUE UN EJECUTOR PROPIO Y NO EL DE POR DEFECTO
 * El de Spring Boot no tiene tope: crea un hilo por tarea. Un registro masivo
 * —o alguien pulsando "reenviar" sin parar— levantaria cientos de hilos
 * bloqueados contra el SMTP y tumbaria la instancia, que en el plan gratuito
 * de Render tiene 512 MB. Este va acotado a mano.
 */
@Configuration
@EnableAsync
public class ConfiguracionAsincrona {

    /**
     * El correo no necesita paralelismo, necesita salir del camino.
     * Dos hilos sobran para el volumen de esta aplicacion y la cola de 100
     * absorbe cualquier pico razonable.
     *
     * NOTA: al declarar un bean de tipo Executor, Spring Boot deja de crear el
     * suyo (applicationTaskExecutor). No afecta a nada aqui porque ningun
     * controlador devuelve Callable ni DeferredResult, que es lo unico que lo
     * usaria. Queda escrito para que no sorprenda si alguna vez se agrega.
     */
    @Bean("ejecutorCorreo")
    public Executor ejecutorCorreo() {
        ThreadPoolTaskExecutor ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(2);
        ejecutor.setMaxPoolSize(4);
        ejecutor.setQueueCapacity(100);
        ejecutor.setThreadNamePrefix("correo-");

        /*
          Si la cola se llena, la tarea la ejecuta el hilo que la envio.

          Es deliberado y es la parte importante de esta clase. La politica por
          omision (AbortPolicy) lanza TaskRejectedException, y esa excepcion
          NO sale en el hilo del ejecutor: sale en el hilo de Tomcat que llamo
          al metodo. O sea que bajo saturacion el registro del usuario se
          romperia con un error 500, justo lo contrario de lo que se busca.

          Con CallerRunsPolicy, el peor caso es volver al comportamiento
          anterior: la peticion espera al SMTP y tarda unos segundos. Lento,
          pero correcto. Degradar hacia "lento" es aceptable; degradar hacia
          "no se pudo crear la cuenta" no lo es.
        */
        ejecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        /*
          Al apagar, se espera a que salgan los correos pendientes.

          Render reinicia el servicio en cada despliegue. Sin esto, los mensajes
          que estuvieran en la cola en ese instante se perderian en silencio y
          alguien se quedaria esperando un codigo que nunca fue enviado. Diez
          segundos es mas que suficiente y no retrasa el despliegue de forma
          apreciable.
        */
        ejecutor.setWaitForTasksToCompleteOnShutdown(true);
        ejecutor.setAwaitTerminationSeconds(10);

        ejecutor.initialize();
        return ejecutor;
    }
}
