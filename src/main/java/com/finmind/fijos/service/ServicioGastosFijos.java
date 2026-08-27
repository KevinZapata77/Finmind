package com.finmind.fijos.service;

import com.finmind.categorias.entity.Categoria;
import com.finmind.categorias.service.ServicioCategorias;
import com.finmind.common.exception.RecursoNoEncontradoException;
import com.finmind.fijos.dto.GastoFijoRequest;
import com.finmind.fijos.dto.GastoFijoResponse;
import com.finmind.fijos.entity.GastoFijo;
import com.finmind.fijos.repository.GastoFijoRepository;
import com.finmind.movimientos.repository.TransaccionRepository;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * RF-046. Compromisos que se repiten.
 *
 * Todo metodo recibe el usuarioId del token, nunca del cuerpo (RN-005).
 */
@Service
public class ServicioGastosFijos {

    private final GastoFijoRepository fijos;
    private final UsuarioRepository usuarios;
    private final ServicioCategorias categorias;
    private final TransaccionRepository movimientos;

    public ServicioGastosFijos(GastoFijoRepository fijos,
                               UsuarioRepository usuarios,
                               ServicioCategorias categorias,
                               TransaccionRepository movimientos) {
        this.fijos = fijos;
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.movimientos = movimientos;
    }

    @Transactional
    public GastoFijoResponse crear(Long usuarioId, GastoFijoRequest p) {
        String nombre = p.nombre().trim();
        if (fijos.existsByUsuarioIdAndNombreIgnoreCase(usuarioId, nombre)) {
            throw new GastoFijoRepetidoException("Ya tienes un gasto fijo con ese nombre");
        }

        Categoria categoria = exigirCategoriaDeGasto(usuarioId, p.categoriaId());
        Usuario dueno = usuarios.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException(
                        "El token es valido pero el usuario ya no existe"));

        GastoFijo g = new GastoFijo(dueno, categoria, nombre, p.monto(),
                periodicidadValida(p.periodicidad()), p.diaPago());

        return aRespuesta(usuarioId, fijos.save(g));
    }

    @Transactional(readOnly = true)
    public List<GastoFijoResponse> listar(Long usuarioId, boolean incluirInactivos) {
        List<GastoFijo> propios = incluirInactivos
                ? fijos.findByUsuarioIdOrderByDiaPagoAsc(usuarioId)
                : fijos.findByUsuarioIdAndActivoTrueOrderByDiaPagoAsc(usuarioId);

        return propios.stream().map(g -> aRespuesta(usuarioId, g)).toList();
    }

    @Transactional
    public GastoFijoResponse actualizar(Long usuarioId, Long id, GastoFijoRequest p) {
        GastoFijo g = buscarPropio(usuarioId, id);
        String nombre = p.nombre().trim();

        if (fijos.existsByUsuarioIdAndNombreIgnoreCaseAndIdNot(usuarioId, nombre, id)) {
            throw new GastoFijoRepetidoException("Ya tienes otro gasto fijo con ese nombre");
        }

        g.editar(exigirCategoriaDeGasto(usuarioId, p.categoriaId()), nombre, p.monto(),
                periodicidadValida(p.periodicidad()), p.diaPago());
        return aRespuesta(usuarioId, g);
    }

    /**
     * No se borra, se desactiva. Un compromiso que ya no aplica sigue
     * explicando las alertas de los meses en que si aplicaba.
     */
    @Transactional
    public GastoFijoResponse desactivar(Long usuarioId, Long id) {
        GastoFijo g = buscarPropio(usuarioId, id);
        g.desactivar();
        return aRespuesta(usuarioId, g);
    }

    @Transactional
    public GastoFijoResponse activar(Long usuarioId, Long id) {
        GastoFijo g = buscarPropio(usuarioId, id);
        g.activar();
        return aRespuesta(usuarioId, g);
    }

    // ------------------------------------------------- para el modulo de alertas

    /**
     * RN-026. Compromisos del mes que todavia NO se han cubierto.
     *
     * Es el numero que hace posible la alerta: lo que falta por pagar. Si un
     * compromiso ya se pago, incluirlo diria que al usuario le falta dinero que
     * en realidad ya gasto.
     */
    @Transactional(readOnly = true)
    public BigDecimal pendienteDelMes(Long usuarioId, YearMonth mes) {
        return fijos.findByUsuarioIdAndActivoTrueOrderByDiaPagoAsc(usuarioId).stream()
                .filter(g -> !estaCubierto(usuarioId, g, mes))
                .map(GastoFijo::montoMensualEquivalente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Los compromisos sin cubrir, para poder nombrarlos en la alerta. */
    @Transactional(readOnly = true)
    public List<GastoFijoResponse> pendientesDelMes(Long usuarioId, YearMonth mes) {
        LocalDate hoy = LocalDate.now();
        return fijos.findByUsuarioIdAndActivoTrueOrderByDiaPagoAsc(usuarioId).stream()
                .filter(g -> !estaCubierto(usuarioId, g, mes))
                .map(g -> GastoFijoResponse.de(g, hoy, false))
                .toList();
    }

    /**
     * RN-026. Un compromiso se considera cubierto cuando el gasto real de su
     * categoria en el mes ya alcanza su monto mensual.
     *
     * Es una aproximacion y conviene decirlo: si el usuario registra el arriendo
     * sin categoria de Vivienda, o mete otro gasto grande en esa categoria, la
     * cuenta se equivoca. La alternativa era pedirle que marcara a mano cada
     * pago, y un aviso que exige mantenimiento manual deja de usarse en dos
     * semanas. Se prefiere una estimacion util a un dato exacto que nadie
     * alimenta.
     */
    private boolean estaCubierto(Long usuarioId, GastoFijo g, YearMonth mes) {
        BigDecimal gastado = movimientos.consumoDeCategoria(
                usuarioId, g.getCategoria().getId(), mes.atDay(1), mes.atEndOfMonth());
        return gastado.compareTo(g.montoMensualEquivalente()) >= 0;
    }

    // ------------------------------------------------------------------ apoyo

    private GastoFijoResponse aRespuesta(Long usuarioId, GastoFijo g) {
        YearMonth mes = YearMonth.now();
        return GastoFijoResponse.de(g, LocalDate.now(), estaCubierto(usuarioId, g, mes));
    }

    private GastoFijo buscarPropio(Long usuarioId, Long id) {
        return fijos.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El gasto fijo no existe"));
    }

    /**
     * Solo categorias de gasto. Un compromiso recurrente de tipo ingreso no
     * significa nada: un sueldo que entra cada mes no es algo que haya que
     * reservar dinero para pagar.
     */
    private Categoria exigirCategoriaDeGasto(Long usuarioId, Long categoriaId) {
        Categoria c = categorias.exigirUsable(usuarioId, categoriaId);
        if (!"GASTO".equals(c.getTipo())) {
            throw new CategoriaDeGastoRequeridaException(
                    "Un gasto fijo tiene que apuntar a una categoria de gasto");
        }
        return c;
    }

    private String periodicidadValida(String valor) {
        String limpio = valor == null ? "" : valor.trim().toUpperCase();
        if (!GastoFijo.PERIODICIDADES.contains(limpio)) {
            throw new PeriodicidadInvalidaException(
                    "La periodicidad debe ser MENSUAL, QUINCENAL o SEMANAL");
        }
        return limpio;
    }

    /** 409: ya existe otro compromiso con ese nombre. */
    public static class GastoFijoRepetidoException extends RuntimeException {
        public GastoFijoRepetidoException(String m) { super(m); }
    }

    /** 400: la periodicidad no esta entre las permitidas. */
    public static class PeriodicidadInvalidaException extends RuntimeException {
        public PeriodicidadInvalidaException(String m) { super(m); }
    }

    /** 400: se apunto a una categoria de ingreso. */
    public static class CategoriaDeGastoRequeridaException extends RuntimeException {
        public CategoriaDeGastoRequeridaException(String m) { super(m); }
    }
}
