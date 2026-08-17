package com.finmind.obligaciones.service;

import com.finmind.common.exception.RecursoNoEncontradoException;
import com.finmind.obligaciones.dto.*;
import com.finmind.obligaciones.entity.Obligacion;
import com.finmind.obligaciones.entity.PagoObligacion;
import com.finmind.obligaciones.repository.ObligacionRepository;
import com.finmind.obligaciones.repository.PagoObligacionRepository;
import com.finmind.usuarios.entity.Usuario;
import com.finmind.usuarios.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Obligaciones financieras (RF-035 a RF-039).
 *
 * El usuario sale del token en todos los metodos, nunca del cuerpo (RN-005).
 */
@Service
public class ServicioObligaciones {

    private static final int AVISO_DIAS = 7;

    private final ObligacionRepository obligaciones;
    private final PagoObligacionRepository pagos;
    private final UsuarioRepository usuarios;

    public ServicioObligaciones(ObligacionRepository obligaciones,
                                PagoObligacionRepository pagos,
                                UsuarioRepository usuarios) {
        this.obligaciones = obligaciones;
        this.pagos = pagos;
        this.usuarios = usuarios;
    }

    // ------------------------------------------------------------ RF-035

    @Transactional
    public ObligacionResponse crear(Long usuarioId, CrearObligacionRequest p) {
        String nombre = p.nombre().trim();
        if (obligaciones.existsByUsuarioIdAndNombreIgnoreCase(usuarioId, nombre)) {
            throw new NombreDeObligacionRepetidoException("Ya tienes una obligacion con ese nombre");
        }
        Usuario dueno = usuarios.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException("El token es valido pero el usuario ya no existe"));

        Obligacion o = new Obligacion(dueno, nombre, p.acreedor().trim(), normalizarTipo(p.tipo()),
                p.montoOriginal(), p.tasaOCero(), p.cuotaMensual(), p.diaPago(), p.fechaInicio());
        return aRespuesta(obligaciones.save(o));
    }

    // ------------------------------------------------------------ RF-036

    /**
     * RN-018 y RN-019. El pago se parte en interes y capital, en ese orden.
     *
     * Si no alcanza a cubrir el interes, el abono a capital queda en cero y la
     * deuda no baja. El sistema lo dice en la respuesta en vez de callarlo: ese
     * es exactamente el caso en que alguien cree que avanza y no avanza.
     */
    @Transactional
    public PagoResponse registrarPago(Long usuarioId, Long obligacionId, RegistrarPagoRequest p) {
        Obligacion o = buscarPropia(usuarioId, obligacionId);

        if (!o.estaActiva()) {
            throw new ObligacionCerradaException(
                    "Esta obligacion ya esta " + o.getEstado().toLowerCase() + " y no admite pagos");
        }

        BigDecimal interes = o.interesDelPeriodo();
        BigDecimal tope = o.totalParaSaldar();
        if (p.monto().compareTo(tope) > 0) {
            throw new PagoExcedeLaDeudaException(
                    "El pago supera lo que debes. Para saldarla por completo basta con " + tope);
        }

        BigDecimal abonoCapital = p.monto().subtract(interes).max(BigDecimal.ZERO);
        // Cuando el pago no cubre el interes, todo el pago se va en interes.
        BigDecimal interesAplicado = p.monto().subtract(abonoCapital);

        o.descontar(abonoCapital);

        PagoObligacion pago = new PagoObligacion(o, p.monto(), interesAplicado, abonoCapital,
                o.getSaldoPendiente(), p.fecha(), p.descripcion());
        return PagoResponse.de(pagos.save(pago));
    }

    @Transactional(readOnly = true)
    public List<PagoResponse> historial(Long usuarioId, Long obligacionId) {
        buscarPropia(usuarioId, obligacionId);   // valida la propiedad antes de listar
        return pagos.findByObligacionIdOrderByFechaDescIdDesc(obligacionId)
                .stream().map(PagoResponse::de).toList();
    }

    // ------------------------------------------------------------ RF-037

    @Transactional(readOnly = true)
    public List<ObligacionResponse> listar(Long usuarioId, boolean soloActivas) {
        List<Obligacion> propias = soloActivas
                ? obligaciones.findByUsuarioIdAndEstadoOrderByNombreAsc(usuarioId, Obligacion.ACTIVA)
                : obligaciones.findByUsuarioIdOrderByNombreAsc(usuarioId);
        return propias.stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public ObligacionResponse consultar(Long usuarioId, Long id) {
        return aRespuesta(buscarPropia(usuarioId, id));
    }

    @Transactional
    public ObligacionResponse actualizar(Long usuarioId, Long id, ActualizarObligacionRequest p) {
        Obligacion o = buscarPropia(usuarioId, id);
        String nombre = p.nombre().trim();
        if (obligaciones.existsByUsuarioIdAndNombreIgnoreCaseAndIdNot(usuarioId, nombre, id)) {
            throw new NombreDeObligacionRepetidoException("Ya tienes otra obligacion con ese nombre");
        }
        o.editar(nombre, p.acreedor().trim(), p.cuotaMensual(), p.diaPago());
        return aRespuesta(o);
    }

    @Transactional
    public ObligacionResponse cancelar(Long usuarioId, Long id) {
        Obligacion o = buscarPropia(usuarioId, id);
        o.cancelar();
        return aRespuesta(o);
    }

    // ------------------------------------------------------------ RF-038

    /**
     * RN-020. El activo suma solo dinero propio; el pasivo, lo que se debe.
     * Las cuentas de tipo TARJETA_CREDITO las excluye ya el modulo de cuentas.
     */
    @Transactional(readOnly = true)
    public PatrimonioResponse patrimonio(Long usuarioId, BigDecimal activos) {
        return PatrimonioResponse.de(activos, obligaciones.totalAdeudado(usuarioId));
    }

    // ------------------------------------------------------------ apoyo

    private Obligacion buscarPropia(Long usuarioId, Long id) {
        return obligaciones.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La obligacion no existe"));
    }

    private String normalizarTipo(String tipo) {
        String limpio = tipo == null ? "" : tipo.trim().toUpperCase();
        if (!Obligacion.TIPOS.contains(limpio)) {
            throw new TipoDeObligacionInvalidoException(
                    "Tipo no valido. Permitidos: " + String.join(", ", Obligacion.TIPOS));
        }
        return limpio;
    }

    /**
     * RF-039. Proxima fecha de pago: el dia pactado de este mes si aun no paso,
     * si no el del mes siguiente. El dia esta limitado a 28 justamente para que
     * la fecha exista en cualquier mes.
     */
    private ObligacionResponse aRespuesta(Obligacion o) {
        LocalDate hoy = LocalDate.now();
        LocalDate proximo = hoy.withDayOfMonth(o.getDiaPago());
        if (proximo.isBefore(hoy)) {
            proximo = proximo.plusMonths(1);
        }
        boolean proxima = o.estaActiva()
                && ChronoUnit.DAYS.between(hoy, proximo) <= AVISO_DIAS;
        return ObligacionResponse.de(o, proximo, proxima);
    }

    /** 409: ya existe otra obligacion con ese nombre. */
    public static class NombreDeObligacionRepetidoException extends RuntimeException {
        public NombreDeObligacionRepetidoException(String m) { super(m); }
    }

    /** 400: el tipo no esta entre los permitidos. */
    public static class TipoDeObligacionInvalidoException extends RuntimeException {
        public TipoDeObligacionInvalidoException(String m) { super(m); }
    }

    /** 409: la obligacion ya esta pagada o cancelada. */
    public static class ObligacionCerradaException extends RuntimeException {
        public ObligacionCerradaException(String m) { super(m); }
    }

    /** 400: el pago es mayor que la deuda total. */
    public static class PagoExcedeLaDeudaException extends RuntimeException {
        public PagoExcedeLaDeudaException(String m) { super(m); }
    }
}
