package com.finmind.common.security;

import com.finmind.common.exception.DemasiadosIntentosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limita cuantas veces se puede repetir una operacion sensible.
 *
 * Cierra tres huecos que tenia la API (SEG-03, SEG-04 y SEG-05):
 *
 *  1. El inicio de sesion no tenia tope. Se podian probar contrasenas sin
 *     freno. BCrypt con coste 12 hacia el ataque lento, pero no imposible.
 *
 *  2. El codigo de verificacion bloquea a los cinco intentos fallidos, pero
 *     se podia pedir un codigo nuevo cuantas veces se quisiera. Gastar cinco
 *     intentos, pedir otro codigo y repetir dejaba el tope sin ningun efecto.
 *     Limitar la EMISION es lo que hace que el limite de intentos signifique
 *     algo: 5 codigos por hora por 5 intentos son 25 combinaciones de un
 *     millon, en lugar de infinitas.
 *
 *  3. Reenviar y recuperar tampoco tenian tope, asi que se podia llenar de
 *     correos la bandeja de cualquier persona y agotar la cuota del proveedor.
 *
 * COMO CUENTA
 * Ventana fija: la primera vez que llega una clave se guarda el momento y se
 * empieza a contar. Cuando la ventana vence, el contador arranca de cero. No
 * es tan preciso como una ventana deslizante — en el cambio de ventana admite
 * hasta el doble de operaciones —, pero para este proposito sobra y se lee sin
 * esfuerzo. Una ventana deslizante exigiria guardar la marca de tiempo de cada
 * intento.
 *
 * LIMITACION QUE HAY QUE CONOCER
 * El contador vive en memoria, asi que solo protege a ESTA instancia. Con dos
 * copias de la aplicacion detras de un balanceador, cada una contaria por su
 * lado y el limite real seria el doble. FinMind se despliega en una sola
 * instancia, asi que alcanza; si algun dia se replica, esto tiene que pasar a
 * un almacen compartido. Queda declarado en el informe de cierre.
 */
@Component
public class LimitadorDeIntentos {

    private static final Logger log = LoggerFactory.getLogger(LimitadorDeIntentos.class);

    /** Cada cuantas operaciones se hace la limpieza de claves vencidas. */
    private static final int CADA_CUANTO_SE_LIMPIA = 500;

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();
    private final AtomicInteger operaciones = new AtomicInteger();

    /**
     * Comprueba el limite y cuenta la operacion.
     *
     * @param clave    identifica lo que se limita. Lleva prefijo para que dos
     *                 flujos distintos no compartan contador: "login:correo",
     *                 "login-ip:1.2.3.4", "codigo:correo".
     * @param maximo   operaciones permitidas dentro de la ventana.
     * @param ventana  cuanto dura la ventana.
     * @param mensaje  lo que se le dice al usuario si se pasa. No debe revelar
     *                 si la cuenta existe.
     * @throws DemasiadosIntentosException si se supera el maximo.
     */
    public void comprobar(String clave, int maximo, Duration ventana, String mensaje) {
        limpiarDeVezEnCuando();

        Instant ahora = Instant.now();
        Registro registro = registros.compute(clave, (k, actual) -> {
            if (actual == null || actual.venceEn().isBefore(ahora)) {
                return new Registro(1, ahora.plus(ventana));
            }
            return new Registro(actual.cuenta() + 1, actual.venceEn());
        });

        if (registro.cuenta() > maximo) {
            // La clave puede contener un correo, asi que no se registra completa.
            log.warn("Limite de intentos alcanzado en {} ({} en la ventana)",
                    soloElPrefijo(clave), registro.cuenta());
            throw new DemasiadosIntentosException(mensaje, segundosQueFaltan(registro, ahora));
        }
    }

    /**
     * Exige que hayan pasado al menos {@code minimo} desde la vez anterior.
     *
     * Se usa para el reenvio de codigos: aparte del tope por hora, dos envios
     * seguidos tienen que estar separados. Sin esto, las cinco emisiones de la
     * hora se podrian gastar en un segundo.
     */
    public void comprobarIntervalo(String clave, Duration minimo, String mensaje) {
        limpiarDeVezEnCuando();

        Instant ahora = Instant.now();
        Registro anterior = registros.get("intervalo:" + clave);

        if (anterior != null && anterior.venceEn().isAfter(ahora)) {
            long faltan = Duration.between(ahora, anterior.venceEn()).toSeconds() + 1;
            log.warn("Reenvio demasiado seguido en {}", soloElPrefijo(clave));
            throw new DemasiadosIntentosException(mensaje, faltan);
        }
        registros.put("intervalo:" + clave, new Registro(1, ahora.plus(minimo)));
    }

    /**
     * Borra el contador de una clave. Se llama cuando la operacion tuvo exito:
     * quien entra bien no debe arrastrar los fallos anteriores.
     */
    public void olvidar(String clave) {
        registros.remove(clave);
    }

    /**
     * Vacia todos los contadores.
     *
     * Existe por las pruebas: el limitador guarda estado entre casos y, sin
     * reiniciarlo, una clase que inicia sesion muchas veces empezaria a fallar
     * por el limite en lugar de por lo que estaba probando. Lo llama
     * LimpiadorDeDatos junto con el borrado de las tablas.
     */
    public void reiniciar() {
        registros.clear();
        operaciones.set(0);
    }

    /** Cuantas claves esta siguiendo. Util para diagnosticar. */
    public int clavesActivas() {
        return registros.size();
    }

    // ------------------------------------------------------------------ interno

    /**
     * Saca las claves ya vencidas. Sin esto el mapa crece indefinidamente y se
     * convierte en una fuga de memoria: cada correo probado deja su entrada.
     * Se hace cada cierto numero de operaciones para no recorrer el mapa en
     * cada peticion.
     */
    private void limpiarDeVezEnCuando() {
        if (operaciones.incrementAndGet() % CADA_CUANTO_SE_LIMPIA != 0) return;
        Instant ahora = Instant.now();
        registros.entrySet().removeIf(e -> e.getValue().venceEn().isBefore(ahora));
    }

    private long segundosQueFaltan(Registro registro, Instant ahora) {
        return Math.max(1, Duration.between(ahora, registro.venceEn()).toSeconds());
    }

    /** Devuelve solo la parte antes de los dos puntos, para no registrar el correo. */
    private String soloElPrefijo(String clave) {
        int i = clave.indexOf(':');
        return i < 0 ? clave : clave.substring(0, i);
    }

    private record Registro(int cuenta, Instant venceEn) {
    }
}
