package com.finmind.common.config;

import com.finmind.identidad.service.ServicioCorreo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El envio de correos sigue saliendo del hilo de la peticion.
 *
 * POR QUE ESTA PRUEBA EXISTE
 * @Async es una anotacion silenciosa: si deja de aplicarse, nada falla, nada
 * se registra en el log y ninguna prueba funcional se pone roja. Lo unico que
 * pasa es que la aplicacion vuelve a tardar cuatro segundos en responder al
 * registro, y eso solo se descubre usandola.
 *
 * Hay al menos tres formas de romperla sin darse cuenta: quitar @EnableAsync,
 * renombrar el bean del ejecutor, o mover la llamada a enviarCodigoVerificacion
 * dentro de la propia clase ServicioCorreo —el proxy de Spring no intercepta
 * las llamadas internas—. Las tres dejan el codigo compilando y las 222
 * pruebas en verde.
 *
 * Por eso aqui no se mide tiempo: medir tiempo seria inestable y ademas solo
 * probaria el sintoma. Se comprueba el mecanismo.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Envio de correo asincrono")
class ConfiguracionAsincronaTest {

    @Autowired
    private ServicioCorreo servicioCorreo;

    @Autowired
    @Qualifier("ejecutorCorreo")
    private Executor ejecutorCorreo;

    @Test
    @DisplayName("el servicio de correo esta envuelto en un proxy")
    void elServicioEstaProxiado() {
        /*
          Sin @EnableAsync, Spring inyecta la instancia pelada y este assert
          falla. Es la comprobacion mas barata de que el mecanismo esta activo.
        */
        assertThat(AopUtils.isAopProxy(servicioCorreo))
                .as("ServicioCorreo deberia estar proxiado; sin proxy, @Async no hace nada")
                .isTrue();
    }

    @Test
    @DisplayName("los dos metodos de envio estan marcados como asincronos")
    void losMetodosDeEnvioSonAsincronos() throws NoSuchMethodException {
        Class<?> real = AopUtils.getTargetClass(servicioCorreo);

        for (String nombre : new String[]{"enviarCodigoVerificacion", "enviarCodigoRecuperacion"}) {
            Method metodo = real.getMethod(nombre, String.class, String.class, String.class, int.class);
            Async async = AnnotatedElementUtils.findMergedAnnotation(metodo, Async.class);

            assertThat(async)
                    .as("%s deberia llevar @Async: si no, la peticion espera al SMTP", nombre)
                    .isNotNull();
            assertThat(async.value())
                    .as("%s deberia usar el ejecutor acotado, no el de por defecto", nombre)
                    .isEqualTo("ejecutorCorreo");
        }
    }

    @Test
    @DisplayName("el ejecutor esta acotado y no rechaza trabajo")
    void elEjecutorEstaAcotado() {
        assertThat(ejecutorCorreo).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) ejecutorCorreo;

        // Acotado: un ejecutor sin tope levantaria un hilo por correo y en una
        // instancia de 512 MB eso termina en OutOfMemory.
        assertThat(pool.getMaxPoolSize()).isLessThanOrEqualTo(4);

        /*
          Y lo mas importante: que ante saturacion la tarea la ejecute quien
          llama, en vez de lanzar TaskRejectedException.

          Esa excepcion saldria en el hilo de Tomcat —no en el del ejecutor— y
          convertiria un pico de registros en errores 500. Con CallerRunsPolicy
          el peor caso es que la peticion tarde lo que tardaba antes.
        */
        assertThat(pool.getThreadPoolExecutor().getRejectedExecutionHandler())
                .as("ante saturacion debe degradar a lento, nunca a error")
                .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
    }

    @Test
    @DisplayName("el envio no revienta cuando el correo esta deshabilitado")
    void elEnvioNoRevientaSinServidorDeCorreo() {
        /*
          En el perfil de prueba finmind.correo.habilitado es false, asi que
          esto no toca ningun servidor: solo confirma que llamar al metodo a
          traves del proxy devuelve el control sin lanzar nada.

          Importa porque el registro de un usuario depende de que este metodo
          jamas propague una excepcion hacia arriba.
        */
        servicioCorreo.enviarCodigoVerificacion(
                "prueba@finmind.test", "Kevin", "123456", 15);
    }
}
