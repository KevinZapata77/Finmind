package com.finmind.common.exception;

/**
 * Se supero el limite de intentos de una operacion sensible.
 *
 * Se traduce a 429 Too Many Requests. El mensaje que lleva nunca revela si la
 * cuenta existe: quien esta al otro lado puede ser el dueno que se equivoco de
 * contrasena o alguien probando direcciones al azar, y a los dos se les
 * responde igual.
 */
public class DemasiadosIntentosException extends RuntimeException {

    /** Cuanto falta para poder volver a intentar. Viaja en el encabezado Retry-After. */
    private final long segundosDeEspera;

    public DemasiadosIntentosException(String mensaje, long segundosDeEspera) {
        super(mensaje);
        this.segundosDeEspera = segundosDeEspera;
    }

    public long getSegundosDeEspera() {
        return segundosDeEspera;
    }
}
