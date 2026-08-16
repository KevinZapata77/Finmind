package com.finmind.cuentas.service;

import com.finmind.common.exception.RecursoNoEncontradoException;
import com.finmind.cuentas.dto.ActualizarCuentaRequest;
import com.finmind.cuentas.dto.CrearCuentaRequest;
import com.finmind.cuentas.dto.CuentaResponse;
import com.finmind.cuentas.entity.Cuenta;
import com.finmind.cuentas.repository.CuentaRepository;
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

    public ServicioCuentas(CuentaRepository cuentas,
                           UsuarioRepository usuarios,
                           CalculadoraDeSaldo calculadora) {
        this.cuentas = cuentas;
        this.usuarios = usuarios;
        this.calculadora = calculadora;
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

        cuenta.editar(nombre, normalizarTipo(peticion.tipo()));
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

    private CuentaResponse aRespuesta(Cuenta cuenta) {
        BigDecimal saldoActual = cuenta.getSaldoInicial()
                .add(calculadora.movimientoNetoDe(cuenta.getId()));
        return CuentaResponse.de(cuenta, saldoActual);
    }

    /** 409: el nombre ya lo usa otra cuenta del mismo usuario. */
    public static class NombreDeCuentaRepetidoException extends RuntimeException {
        public NombreDeCuentaRepetidoException(String mensaje) { super(mensaje); }
    }

    /** 400: el tipo no esta entre los seis permitidos. */
    public static class TipoDeCuentaInvalidoException extends RuntimeException {
        public TipoDeCuentaInvalidoException(String mensaje) { super(mensaje); }
    }
}
