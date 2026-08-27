package com.finmind.cuentas.service;

import com.finmind.common.exception.RecursoNoEncontradoException;
import com.finmind.cuentas.dto.AbonoTarjetaRequest;
import com.finmind.cuentas.dto.ActualizarCuentaRequest;
import com.finmind.cuentas.dto.CrearCuentaRequest;
import com.finmind.cuentas.dto.CuentaResponse;
import com.finmind.cuentas.entity.Cuenta;
import com.finmind.cuentas.repository.CuentaRepository;
import com.finmind.movimientos.entity.Transaccion;
import com.finmind.movimientos.repository.TransaccionRepository;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reglas del modulo de cuentas (RF-006 a RF-008).
 *
 * Todos los metodos reciben el usuarioId del token, nunca del cliente (RN-005).
 * Ningun metodo acepta un usuarioId como dato de entrada del cuerpo.
 */
@Service
public class ServicioCuentas {

    private final CuentaRepository cuentas;
    private final UsuarioRepository usuarios;
    private final CalculadoraDeSaldo calculadora;
    private final TransaccionRepository movimientos;

    public ServicioCuentas(CuentaRepository cuentas,
                           UsuarioRepository usuarios,
                           CalculadoraDeSaldo calculadora,
                           TransaccionRepository movimientos) {
        this.cuentas = cuentas;
        this.usuarios = usuarios;
        this.calculadora = calculadora;
        this.movimientos = movimientos;
    }

    /**
     * RF-045. Abona a una tarjeta de credito desde otra cuenta.
     *
     * Se guarda como una TRANSFERENCIA, no como un ingreso. La diferencia
     * importa: el balance del mes suma los movimientos de tipo INGRESO sin
     * mirar la cuenta, asi que registrar el abono como ingreso hacia que la
     * aplicacion reportara mas ingresos de los que el usuario tuvo. Pagar una
     * deuda no es ganar dinero (DEF-16).
     *
     * Una sola fila deja las dos puntas resueltas: sale de la cuenta de origen
     * y entra a la tarjeta.
     */
    @Transactional
    public CuentaResponse abonar(Long usuarioId, Long tarjetaId, AbonoTarjetaRequest peticion) {
        Cuenta tarjeta = buscarPropia(usuarioId, tarjetaId);
        Cuenta origen = buscarPropia(usuarioId, peticion.cuentaOrigenId());

        if (!tarjeta.esPasivo()) {
            throw new AbonoInvalidoException(
                    "Solo se abona a tarjetas de credito. Para mover dinero entre otras cuentas, "
                            + "registra el movimiento desde Movimientos");
        }
        if (origen.getId().equals(tarjeta.getId())) {
            throw new AbonoInvalidoException("La tarjeta no puede pagarse a si misma");
        }
        // Pagar una tarjeta con otra tarjeta no reduce la deuda, la traslada. Es
        // una operacion real de la banca, pero modelarla bien exige mas de lo que
        // aporta aqui, y admitirla a medias daria numeros que no cuadran.
        if (origen.esPasivo()) {
            throw new AbonoInvalidoException(
                    "El abono tiene que salir de una cuenta de dinero, no de otra tarjeta");
        }
        if (!tarjeta.estaActiva() || !origen.estaActiva()) {
            throw new CuentaDesactivadaException(
                    "No se puede abonar con una cuenta desactivada");
        }

        Usuario dueno = usuarios.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException(
                        "El token es valido pero el usuario ya no existe"));

        movimientos.save(Transaccion.transferencia(
                dueno, origen, tarjeta,
                peticion.monto(), peticion.fecha(),
                peticion.descripcion() == null || peticion.descripcion().isBlank()
                        ? "Abono a " + tarjeta.getNombre()
                        : peticion.descripcion().trim()));

        // Se devuelve la tarjeta ya recalculada: el cliente necesita ver la
        // deuda nueva sin tener que volver a pedirla.
        return aRespuesta(tarjeta);
    }

    @Transactional
    public CuentaResponse crear(Long usuarioId, CrearCuentaRequest peticion) {
        String nombre = peticion.nombre().trim();
        String tipo = normalizarTipo(peticion.tipo());

        if (cuentas.existsByUsuarioIdAndNombreIgnoreCase(usuarioId, nombre)) {
            throw new NombreDeCuentaRepetidoException(
                    "Ya tienes una cuenta con ese nombre");
        }

        Usuario dueno = usuarios.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException(
                        "El token es valido pero el usuario ya no existe"));

        Cuenta cuenta = new Cuenta(dueno, nombre, tipo,
                peticion.saldoInicialOCero(), peticion.monedaOPorDefecto());
        cuenta.cambiarCupo(cupoValidado(tipo, peticion.cupo()));

        return aRespuesta(cuentas.save(cuenta));
    }

    @Transactional(readOnly = true)
    public List<CuentaResponse> listar(Long usuarioId, boolean incluirInactivas) {
        List<Cuenta> propias = incluirInactivas
                ? cuentas.findByUsuarioIdOrderByNombreAsc(usuarioId)
                : cuentas.findByUsuarioIdAndActivaTrueOrderByNombreAsc(usuarioId);

        return propias.stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public CuentaResponse consultar(Long usuarioId, Long cuentaId) {
        return aRespuesta(buscarPropia(usuarioId, cuentaId));
    }

    @Transactional
    public CuentaResponse actualizar(Long usuarioId, Long cuentaId, ActualizarCuentaRequest peticion) {
        Cuenta cuenta = buscarPropia(usuarioId, cuentaId);
        String nombre = peticion.nombre().trim();

        if (cuentas.existsByUsuarioIdAndNombreIgnoreCaseAndIdNot(usuarioId, nombre, cuentaId)) {
            throw new NombreDeCuentaRepetidoException("Ya tienes otra cuenta con ese nombre");
        }

        String tipo = normalizarTipo(peticion.tipo());
        cuenta.editar(nombre, tipo);
        // Si la cuenta deja de ser tarjeta, el cupo se va con ella.
        cuenta.cambiarCupo(cupoValidado(tipo, peticion.cupo()));
        return aRespuesta(cuenta);
    }

    @Transactional
    public CuentaResponse desactivar(Long usuarioId, Long cuentaId) {
        Cuenta cuenta = buscarPropia(usuarioId, cuentaId);
        cuenta.desactivar();
        return aRespuesta(cuenta);
    }

    @Transactional
    public CuentaResponse activar(Long usuarioId, Long cuentaId) {
        Cuenta cuenta = buscarPropia(usuarioId, cuentaId);
        cuenta.activar();
        return aRespuesta(cuenta);
    }

    /**
     * RN-020. Suma solo el dinero que el usuario TIENE.
     *
     * Las cuentas de tipo TARJETA_CREDITO quedan fuera: su saldo no es dinero
     * disponible sino deuda, y sumarlo le informaria al usuario mas plata de la
     * que realmente tiene. Esas deudas viven en el modulo de obligaciones.
     */
    @Transactional(readOnly = true)
    public BigDecimal totalActivos(Long usuarioId) {
        return cuentas.findByUsuarioIdAndActivaTrueOrderByNombreAsc(usuarioId).stream()
                .filter(c -> !c.esPasivo())
                .map(this::saldoDe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * RN-021. Lo que se debe en tarjetas de credito.
     *
     * Existe porque antes esta deuda no aparecia en ningun lado: totalActivos
     * la excluye, y lo adeudado solo miraba el modulo de obligaciones. Una
     * tarjeta registrada como cuenta quedaba invisible en el patrimonio, ni
     * sumaba ni restaba. Ahora el patrimonio la resta (DEF-14).
     */
    @Transactional(readOnly = true)
    public BigDecimal totalDeudaEnTarjetas(Long usuarioId) {
        return cuentas.findByUsuarioIdAndActivaTrueOrderByNombreAsc(usuarioId).stream()
                .filter(Cuenta::esPasivo)
                .map(this::saldoDe)
                // Una tarjeta pagada de mas quedaria en negativo. Eso es saldo a
                // favor, no deuda, asi que no se resta de lo que se debe.
                .map(saldo -> saldo.max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * RN-005. Una cuenta ajena responde "no existe", no "no puedes".
     * Distinguirlos revelaria que esa cuenta existe y de quien es.
     */
    private Cuenta buscarPropia(Long usuarioId, Long cuentaId) {
        return cuentas.findByIdAndUsuarioId(cuentaId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta no existe"));
    }

    private String normalizarTipo(String tipo) {
        String limpio = tipo == null ? "" : tipo.trim().toUpperCase();
        if (!Cuenta.TIPOS.contains(limpio)) {
            throw new TipoDeCuentaInvalidoException(
                    "El tipo de cuenta no es valido. Valores permitidos: " + String.join(", ", Cuenta.TIPOS));
        }
        return limpio;
    }

    /**
     * El signo lo decide la entidad, no este servicio (RN-021).
     *
     * Antes aqui se sumaba el neto siempre, lo que en una tarjeta de credito
     * hacia que comprar bajara la deuda (DEF-13). Ahora Cuenta.saldoCon()
     * resuelve el signo segun el tipo, y es el unico sitio donde se decide.
     */
    private CuentaResponse aRespuesta(Cuenta cuenta) {
        BigDecimal saldoActual = cuenta.saldoCon(calculadora.movimientoNetoDe(cuenta.getId()));

        // El total abonado solo se consulta en las tarjetas. En una cuenta
        // normal un ingreso es dinero que entra, no un pago, asi que el dato no
        // significaria nada y seria una consulta de mas en cada cuenta listada.
        BigDecimal totalPagado = cuenta.esPasivo()
                ? calculadora.totalAbonadoA(cuenta.getId())
                : null;

        return CuentaResponse.de(cuenta, saldoActual, totalPagado);
    }

    /** El saldo de una cuenta, con el signo que le corresponda por su tipo. */
    private BigDecimal saldoDe(Cuenta cuenta) {
        return cuenta.saldoCon(calculadora.movimientoNetoDe(cuenta.getId()));
    }

    /**
     * RN-021. Solo las tarjetas de credito llevan cupo.
     *
     * Aceptar un cupo en una cuenta de ahorros lo dejaria guardado sin que
     * signifique nada, y despues nadie sabria si fue intencional o un descuido.
     * La base tambien lo impide, pero el error del motor no le dice nada util
     * a quien esta llenando el formulario.
     */
    private BigDecimal cupoValidado(String tipo, BigDecimal cupo) {
        if (cupo == null) return null;
        if (!Cuenta.TARJETA_CREDITO.equals(tipo)) {
            throw new CupoSoloEnTarjetasException(
                    "El cupo solo aplica a las tarjetas de credito");
        }
        return cupo;
    }

    /** 409: el nombre ya lo usa otra cuenta del mismo usuario. */
    public static class NombreDeCuentaRepetidoException extends RuntimeException {
        public NombreDeCuentaRepetidoException(String mensaje) { super(mensaje); }
    }

    /** 400: el tipo no esta entre los seis permitidos. */
    public static class TipoDeCuentaInvalidoException extends RuntimeException {
        public TipoDeCuentaInvalidoException(String mensaje) { super(mensaje); }
    }

    /** 400: se envio cupo en una cuenta que no es tarjeta de credito. */
    public static class CupoSoloEnTarjetasException extends RuntimeException {
        public CupoSoloEnTarjetasException(String mensaje) { super(mensaje); }
    }

    /** 400: el abono no cumple las reglas de origen y destino (RF-045). */
    public static class AbonoInvalidoException extends RuntimeException {
        public AbonoInvalidoException(String mensaje) { super(mensaje); }
    }

    /** 409: se intento operar con una cuenta desactivada. */
    public static class CuentaDesactivadaException extends RuntimeException {
        public CuentaDesactivadaException(String mensaje) { super(mensaje); }
    }
}
