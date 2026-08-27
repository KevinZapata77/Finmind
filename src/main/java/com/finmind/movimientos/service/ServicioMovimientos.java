package com.finmind.movimientos.service;

import com.finmind.categorias.entity.Categoria;
import com.finmind.categorias.service.ServicioCategorias;
import com.finmind.common.exception.RecursoNoEncontradoException;
import com.finmind.cuentas.entity.Cuenta;
import com.finmind.cuentas.repository.CuentaRepository;
import com.finmind.movimientos.dto.*;
import com.finmind.movimientos.entity.Transaccion;
import com.finmind.movimientos.repository.TransaccionRepository;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Ingresos y gastos (RF-012 a RF-016).
 *
 * Cada movimiento cruza tres cosas del usuario: cuenta, categoria y el usuario
 * mismo. Las tres se validan contra el token, nunca contra lo que mande el
 * cliente (RN-005).
 */
@Service
public class ServicioMovimientos {

    private static final int TAMANO_MAXIMO = 100;

    private final TransaccionRepository movimientos;
    private final CuentaRepository cuentas;
    private final ServicioCategorias categorias;
    private final UsuarioRepository usuarios;

    public ServicioMovimientos(TransaccionRepository movimientos, CuentaRepository cuentas,
                               ServicioCategorias categorias, UsuarioRepository usuarios) {
        this.movimientos = movimientos;
        this.cuentas = cuentas;
        this.categorias = categorias;
        this.usuarios = usuarios;
    }

    @Transactional
    public MovimientoResponse registrar(Long usuarioId, MovimientoRequest p) {
        Cuenta cuenta = exigirCuenta(usuarioId, p.cuentaId());
        Categoria categoria = categorias.exigirUsable(usuarioId, p.categoriaId());
        Usuario dueno = usuarios.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException("El token es valido pero el usuario ya no existe"));

        exigirQueNoSeaIngresoEnTarjeta(cuenta, categoria);

        Transaccion t = new Transaccion(dueno, cuenta, categoria, p.monto(), p.fecha(), p.descripcion());
        return MovimientoResponse.de(movimientos.save(t));
    }

    @Transactional(readOnly = true)
    public PaginaMovimientos listar(Long usuarioId, LocalDate desde, LocalDate hasta,
                                    Long cuentaId, Long categoriaId, String tipo,
                                    int pagina, int tamano) {
        int limite = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
        String tipoNorm = (tipo == null || tipo.isBlank()) ? null : tipo.trim().toUpperCase();

        Page<Transaccion> pag = movimientos.buscar(usuarioId, desde, hasta, cuentaId,
                categoriaId, tipoNorm, PageRequest.of(Math.max(pagina, 0), limite));

        // Los totales se calculan sobre el rango completo, no sobre la pagina.
        LocalDate d = desde != null ? desde : LocalDate.of(1900, 1, 1);
        LocalDate h = hasta != null ? hasta : LocalDate.of(2999, 12, 31);
        BigDecimal ingresos = movimientos.totalPorTipo(usuarioId, Transaccion.INGRESO, d, h);
        BigDecimal gastos = movimientos.totalPorTipo(usuarioId, Transaccion.GASTO, d, h);

        List<MovimientoResponse> filas = pag.getContent().stream().map(MovimientoResponse::de).toList();
        return PaginaMovimientos.de(pag, filas, ingresos, gastos);
    }

    @Transactional(readOnly = true)
    public MovimientoResponse consultar(Long usuarioId, Long id) {
        return MovimientoResponse.de(exigirPropio(usuarioId, id));
    }

    @Transactional
    public MovimientoResponse actualizar(Long usuarioId, Long id, MovimientoRequest p) {
        Transaccion t = exigirPropio(usuarioId, id);

        // RN-022. Una transferencia no se edita por aqui. Este metodo le asigna
        // una categoria y deduce el tipo de ella, lo que convertiria la
        // transferencia en un ingreso o un gasto pero dejaria puesta la cuenta
        // de destino: una fila que ninguna consulta sabria interpretar, y que la
        // base rechazaria con un error tecnico incomprensible para el usuario.
        // Para corregir un abono equivocado se borra y se registra de nuevo.
        if (t.esTransferencia()) {
            throw new TransferenciaNoEditableException(
                    "Un abono no se edita. Si te equivocaste, borralo y registralo de nuevo");
        }

        Cuenta cuenta = exigirCuenta(usuarioId, p.cuentaId());
        Categoria categoria = categorias.exigirUsable(usuarioId, p.categoriaId());
        exigirQueNoSeaIngresoEnTarjeta(cuenta, categoria);
        t.editar(cuenta, categoria, p.monto(), p.fecha(), p.descripcion());
        return MovimientoResponse.de(t);
    }

    /**
     * RF-016. Aqui si se borra de verdad, a diferencia de cuentas y categorias.
     * Un movimiento equivocado no es historia que preservar: es un dato falso,
     * y dejarlo "inactivo" descuadraria el saldo o exigiria filtrarlo en cada
     * consulta para siempre.
     */
    @Transactional
    public void eliminar(Long usuarioId, Long id) {
        movimientos.delete(exigirPropio(usuarioId, id));
    }

    /**
     * RN-023. Un ingreso sobre una tarjeta de credito no se admite.
     *
     * Registrarlo bajaba la deuda, y por eso era la unica forma de "pagar" la
     * tarjeta antes de que existieran las transferencias. Pero tenia dos
     * consecuencias que hacian mentir a la aplicacion:
     *
     *   - Inflaba los ingresos del mes: el balance suma los INGRESO sin mirar
     *     la cuenta, asi que un abono de 200.000 aparecia como si el usuario
     *     hubiera ganado 200.000 mas.
     *   - El dinero no salia de ninguna parte: la deuda bajaba sin descontarse
     *     de la cuenta de donde salio el pago.
     *
     * Ahora existe el abono (RF-045), que hace las dos cosas bien. Dejar
     * abierta la via antigua significaria mantener las dos, y la incorrecta
     * seria la mas facil de encontrar por descuido.
     */
    private void exigirQueNoSeaIngresoEnTarjeta(Cuenta cuenta, Categoria categoria) {
        if (cuenta.esPasivo() && Transaccion.INGRESO.equals(categoria.getTipo())) {
            throw new IngresoEnTarjetaException(
                    "Para pagar una tarjeta usa el boton Abonar en la pantalla de Cuentas: "
                            + "asi el dinero se descuenta de la cuenta de donde sale y no se "
                            + "cuenta como un ingreso del mes");
        }
    }

    private Transaccion exigirPropio(Long usuarioId, Long id) {
        return movimientos.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El movimiento no existe"));
    }

    private Cuenta exigirCuenta(Long usuarioId, Long cuentaId) {
        Cuenta c = cuentas.findByIdAndUsuarioId(cuentaId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta no existe"));
        if (!c.estaActiva()) {
            throw new CuentaInactivaException(
                    "La cuenta '" + c.getNombre() + "' esta desactivada. Reactívala o elige otra.");
        }
        return c;
    }

    /** 409: se intento mover plata en una cuenta desactivada. */
    public static class CuentaInactivaException extends RuntimeException {
        public CuentaInactivaException(String m) { super(m); }
    }

    /** 409: se intento editar una transferencia desde el modulo de movimientos. */
    public static class TransferenciaNoEditableException extends RuntimeException {
        public TransferenciaNoEditableException(String mensaje) { super(mensaje); }
    }

    /** 400: RN-023, se intento registrar un ingreso sobre una tarjeta de credito. */
    public static class IngresoEnTarjetaException extends RuntimeException {
        public IngresoEnTarjetaException(String mensaje) { super(mensaje); }
    }
}
