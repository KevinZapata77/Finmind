package com.finmind.presupuestos.service;

import com.finmind.categorias.entity.Categoria;
import com.finmind.categorias.service.ServicioCategorias;
import com.finmind.common.exception.RecursoNoEncontradoException;
import com.finmind.movimientos.repository.TransaccionRepository;
import com.finmind.presupuestos.dto.*;
import com.finmind.presupuestos.entity.Presupuesto;
import com.finmind.presupuestos.repository.PresupuestoRepository;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Presupuestos por categoria y mes (RF-017 a RF-020). */
@Service
public class ServicioPresupuestos {

    private final PresupuestoRepository presupuestos;
    private final TransaccionRepository movimientos;
    private final ServicioCategorias categorias;
    private final UsuarioRepository usuarios;

    public ServicioPresupuestos(PresupuestoRepository presupuestos,
                                TransaccionRepository movimientos,
                                ServicioCategorias categorias,
                                UsuarioRepository usuarios) {
        this.presupuestos = presupuestos;
        this.movimientos = movimientos;
        this.categorias = categorias;
        this.usuarios = usuarios;
    }

    @Transactional
    public PresupuestoResponse crear(Long usuarioId, PresupuestoRequest p) {
        Categoria categoria = categorias.exigirUsable(usuarioId, p.categoriaId());

        // Un presupuesto de INGRESO no significa nada: no se limita lo que entra.
        if (!Categoria.GASTO.equals(categoria.getTipo())) {
            throw new PresupuestoSoloDeGastoException(
                    "Solo se presupuestan categorias de gasto. '" + categoria.getNombre()
                            + "' es de ingreso.");
        }
        // RN-006
        if (presupuestos.existsByUsuarioIdAndCategoriaIdAndAnioAndMes(
                usuarioId, categoria.getId(), p.anio(), p.mes())) {
            throw new PresupuestoRepetidoException(
                    "Ya tienes un presupuesto de '" + categoria.getNombre()
                            + "' para " + p.mes() + "/" + p.anio());
        }
        String periodo = p.periodoOMensual();
        if (!Presupuesto.PERIODOS.contains(periodo)) {
            throw new PeriodoInvalidoException("El periodo debe ser MENSUAL, QUINCENAL o SEMANAL");
        }

        Usuario dueno = usuarios.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException("El token es valido pero el usuario ya no existe"));

        Presupuesto nuevo = presupuestos.save(
                new Presupuesto(dueno, categoria, p.montoLimite(), periodo, p.anio(), p.mes()));
        return conConsumo(usuarioId, nuevo);
    }

    /** RF-018. Si no se indica periodo, se toma el mes en curso. */
    @Transactional(readOnly = true)
    public List<PresupuestoResponse> listar(Long usuarioId, Short anio, Short mes) {
        YearMonth actual = YearMonth.now();
        short a = anio != null ? anio : (short) actual.getYear();
        short m = mes != null ? mes : (short) actual.getMonthValue();

        return presupuestos.findByUsuarioIdAndAnioAndMesOrderByCategoriaNombreAsc(usuarioId, a, m)
                .stream().map(p -> conConsumo(usuarioId, p)).toList();
    }

    @Transactional(readOnly = true)
    public PresupuestoResponse consultar(Long usuarioId, Long id) {
        return conConsumo(usuarioId, exigirPropio(usuarioId, id));
    }

    @Transactional
    public PresupuestoResponse editar(Long usuarioId, Long id, EditarPresupuestoRequest p) {
        Presupuesto presupuesto = exigirPropio(usuarioId, id);
        presupuesto.editar(p.montoLimite());
        return conConsumo(usuarioId, presupuesto);
    }

    @Transactional
    public PresupuestoResponse desactivar(Long usuarioId, Long id) {
        Presupuesto presupuesto = exigirPropio(usuarioId, id);
        presupuesto.desactivar();
        return conConsumo(usuarioId, presupuesto);
    }

    @Transactional
    public PresupuestoResponse activar(Long usuarioId, Long id) {
        Presupuesto presupuesto = exigirPropio(usuarioId, id);
        presupuesto.activar();
        return conConsumo(usuarioId, presupuesto);
    }

    /**
     * RN-009. El consumo se calcula, no se guarda: asi siempre cuadra con los
     * movimientos que el usuario puede ver en pantalla.
     */
    private PresupuestoResponse conConsumo(Long usuarioId, Presupuesto p) {
        YearMonth periodo = YearMonth.of(p.getAnio(), p.getMes());
        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();
        BigDecimal consumo = movimientos.consumoDeCategoria(
                usuarioId, p.getCategoria().getId(), desde, hasta);
        return PresupuestoResponse.de(p, consumo);
    }

    private Presupuesto exigirPropio(Long usuarioId, Long id) {
        return presupuestos.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El presupuesto no existe"));
    }

    /** 409 */
    public static class PresupuestoRepetidoException extends RuntimeException {
        public PresupuestoRepetidoException(String m) { super(m); }
    }
    /** 400 */
    public static class PresupuestoSoloDeGastoException extends RuntimeException {
        public PresupuestoSoloDeGastoException(String m) { super(m); }
    }
    /** 400 */
    public static class PeriodoInvalidoException extends RuntimeException {
        public PeriodoInvalidoException(String m) { super(m); }
    }
}
