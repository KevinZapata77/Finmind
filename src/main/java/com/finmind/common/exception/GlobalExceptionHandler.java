package com.finmind.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import com.finmind.identidad.service.ServicioIdentidad.CodigoInvalidoException;
import com.finmind.identidad.service.ServicioCaptcha.CaptchaInvalidoException;
import com.finmind.identidad.service.ServicioUsuarioGoogle.CuentaGoogleException;
import com.finmind.cuentas.service.ServicioCuentas.NombreDeCuentaRepetidoException;
import com.finmind.cuentas.service.ServicioCuentas.TipoDeCuentaInvalidoException;
import com.finmind.cuentas.service.ServicioCuentas.CupoSoloEnTarjetasException;
import com.finmind.cuentas.service.ServicioCuentas.AbonoInvalidoException;
import com.finmind.cuentas.service.ServicioCuentas.CuentaDesactivadaException;
import com.finmind.obligaciones.service.ServicioObligaciones.NombreDeObligacionRepetidoException;
import com.finmind.obligaciones.service.ServicioObligaciones.TipoDeObligacionInvalidoException;
import com.finmind.obligaciones.service.ServicioObligaciones.ObligacionCerradaException;
import com.finmind.obligaciones.service.ServicioObligaciones.PagoExcedeLaDeudaException;
import com.finmind.categorias.service.ServicioCategorias.CategoriaRepetidaException;
import com.finmind.categorias.service.ServicioCategorias.TipoDeCategoriaInvalidoException;
import com.finmind.categorias.service.ServicioCategorias.CategoriaInactivaException;
import com.finmind.movimientos.service.ServicioMovimientos.CuentaInactivaException;
import com.finmind.movimientos.service.ServicioMovimientos.TransferenciaNoEditableException;
import com.finmind.movimientos.service.ServicioMovimientos.IngresoEnTarjetaException;
import com.finmind.presupuestos.service.ServicioPresupuestos.PresupuestoRepetidoException;
import com.finmind.presupuestos.service.ServicioPresupuestos.PresupuestoSoloDeGastoException;
import com.finmind.presupuestos.service.ServicioPresupuestos.PeriodoInvalidoException;
import com.finmind.fijos.service.ServicioGastosFijos.GastoFijoRepetidoException;
import com.finmind.fijos.service.ServicioGastosFijos.PeriodicidadInvalidaException;
import com.finmind.fijos.service.ServicioGastosFijos.CategoriaDeGastoRequeridaException;
import com.finmind.fijos.service.ServicioGastosFijos.DiaDePagoInvalidoException;
import com.finmind.metas.service.ServicioMetas.MetaRepetidaException;
import com.finmind.metas.service.ServicioMetas.MetaCerradaException;
import com.finmind.metas.service.ServicioMetas.AbonoExcesivoException;
import com.finmind.metas.service.ServicioMetas.ObjetivoMenorQueLoAhorradoException;
import com.finmind.administracion.service.ServicioAdministracion.AdminNoSePuedeDesactivarException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejo centralizado de errores (API-03).
 *
 * Ninguna respuesta expone trazas internas ni nombres de clases. Lo que el
 * cliente recibe es un ApiError uniforme; el detalle tecnico va al log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    /**
     * 429: se supero el limite de intentos (SEG-03, SEG-04, SEG-05).
     *
     * Lleva el encabezado Retry-After con los segundos que faltan, que es lo
     * que la norma define para este codigo y lo que permite al cliente mostrar
     * una cuenta atras en lugar de un error seco.
     */
    @ExceptionHandler(DemasiadosIntentosException.class)
    public ResponseEntity<ApiError> demasiadosIntentos(DemasiadosIntentosException ex,
                                                       HttpServletRequest req) {
        ApiError cuerpo = new ApiError(
                OffsetDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                ex.getMessage(),
                req.getRequestURI(),
                null);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getSegundosDeEspera()))
                .body(cuerpo);
    }

    @ExceptionHandler(CorreoYaRegistradoException.class)
    public ResponseEntity<ApiError> correoDuplicado(CorreoYaRegistradoException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /**
     * Credenciales invalidas y usuario inexistente devuelven exactamente la misma
     * respuesta. Distinguirlas permitiria averiguar que correos estan registrados.
     */
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ApiError> credencialesInvalidas(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Correo o contrasena incorrectos", req, null);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> cuentaInactiva(DisabledException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "La cuenta se encuentra inactiva", req, null);
    }

    /** RN-011: cuenta con correo sin verificar. */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiError> correoSinVerificar(LockedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN,
                "Debes verificar tu correo antes de iniciar sesion.", req, null);
    }

    /** Codigo inexistente, vencido, equivocado o con los intentos agotados. */
    @ExceptionHandler(CodigoInvalidoException.class)
    public ResponseEntity<ApiError> codigoInvalido(CodigoInvalidoException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    /** El CAPTCHA falta, es falso o el proveedor lo rechazo. */
    @ExceptionHandler(CaptchaInvalidoException.class)
    public ResponseEntity<ApiError> captchaInvalido(CaptchaInvalidoException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    /** Conflicto entre una cuenta de Google y una cuenta local con el mismo correo. */
    @ExceptionHandler(CuentaGoogleException.class)
    public ResponseEntity<ApiError> cuentaGoogle(CuentaGoogleException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /** 409: el usuario ya tiene una cuenta con ese nombre (RF-006). */
    @ExceptionHandler(NombreDeCuentaRepetidoException.class)
    public ResponseEntity<ApiError> nombreDeCuentaRepetido(NombreDeCuentaRepetidoException ex,
                                                           HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /** 400: el tipo de cuenta no esta entre los permitidos. */
    @ExceptionHandler(TipoDeCuentaInvalidoException.class)
    public ResponseEntity<ApiError> tipoDeCuentaInvalido(TipoDeCuentaInvalidoException ex,
                                                         HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    /** 409: RF-046, ya existe otro gasto fijo con ese nombre. */
    @ExceptionHandler(GastoFijoRepetidoException.class)
    public ResponseEntity<ApiError> gastoFijoRepetido(GastoFijoRepetidoException ex,
                                                      HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /** 400: RF-046, periodicidad, categoria o dia de pago no validos en un gasto fijo. */
    @ExceptionHandler({PeriodicidadInvalidaException.class,
                       CategoriaDeGastoRequeridaException.class,
                       DiaDePagoInvalidoException.class})
    public ResponseEntity<ApiError> gastoFijoInvalido(RuntimeException ex,
                                                      HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    /** 400: RN-023, un ingreso sobre una tarjeta se hace con el abono. */
    @ExceptionHandler(IngresoEnTarjetaException.class)
    public ResponseEntity<ApiError> ingresoEnTarjeta(IngresoEnTarjetaException ex,
                                                     HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    /** 409: RN-022, una transferencia no se edita, se borra y se rehace. */
    @ExceptionHandler(TransferenciaNoEditableException.class)
    public ResponseEntity<ApiError> transferenciaNoEditable(TransferenciaNoEditableException ex,
                                                            HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /** 400: RF-045, el abono no cumple las reglas de origen y destino. */
    @ExceptionHandler(AbonoInvalidoException.class)
    public ResponseEntity<ApiError> abonoInvalido(AbonoInvalidoException ex,
                                                  HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    /** 409: se intento operar con una cuenta desactivada. */
    @ExceptionHandler(CuentaDesactivadaException.class)
    public ResponseEntity<ApiError> cuentaDesactivada(CuentaDesactivadaException ex,
                                                      HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /** 400: RN-021, se envio cupo en una cuenta que no es tarjeta de credito. */
    @ExceptionHandler(CupoSoloEnTarjetasException.class)
    public ResponseEntity<ApiError> cupoSoloEnTarjetas(CupoSoloEnTarjetasException ex,
                                                       HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    /** 409: ya existe otra obligacion con ese nombre (RF-035). */
    @ExceptionHandler(NombreDeObligacionRepetidoException.class)
    public ResponseEntity<ApiError> obligacionRepetida(NombreDeObligacionRepetidoException ex,
                                                       HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /** 409: la obligacion ya esta pagada o cancelada (RF-036). */
    @ExceptionHandler(ObligacionCerradaException.class)
    public ResponseEntity<ApiError> obligacionCerrada(ObligacionCerradaException ex,
                                                      HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /** 400: tipo de obligacion no permitido. */
    @ExceptionHandler(TipoDeObligacionInvalidoException.class)
    public ResponseEntity<ApiError> tipoObligacion(TipoDeObligacionInvalidoException ex,
                                                   HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    /** 400: el pago supera la deuda total (RF-036). */
    @ExceptionHandler(PagoExcedeLaDeudaException.class)
    public ResponseEntity<ApiError> pagoExcesivo(PagoExcedeLaDeudaException ex,
                                                 HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    /** 409: ya existe una categoria con ese nombre y tipo (RF-009). */
    @ExceptionHandler(CategoriaRepetidaException.class)
    public ResponseEntity<ApiError> categoriaRepetida(CategoriaRepetidaException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /** 400: el tipo de categoria no es INGRESO ni GASTO. */
    @ExceptionHandler(TipoDeCategoriaInvalidoException.class)
    public ResponseEntity<ApiError> tipoCategoria(TipoDeCategoriaInvalidoException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    /** 409: se intento usar una categoria o una cuenta desactivada (RF-012). */
    @ExceptionHandler({CategoriaInactivaException.class, CuentaInactivaException.class})
    public ResponseEntity<ApiError> recursoInactivo(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /** 409: RN-006, ya hay un presupuesto de esa categoria para ese mes. */
    @ExceptionHandler(PresupuestoRepetidoException.class)
    public ResponseEntity<ApiError> presupuestoRepetido(PresupuestoRepetidoException ex,
                                                        HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /** 400: se intento presupuestar una categoria de ingreso o un periodo invalido. */
    @ExceptionHandler({PresupuestoSoloDeGastoException.class, PeriodoInvalidoException.class})
    public ResponseEntity<ApiError> presupuestoInvalido(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    /** 409: conflictos de metas y de administracion. */
    @ExceptionHandler({MetaRepetidaException.class, MetaCerradaException.class,
                       AdminNoSePuedeDesactivarException.class})
    public ResponseEntity<ApiError> conflictoDeNegocio(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    /** 400: el abono o el objetivo de una meta no son coherentes. */
    @ExceptionHandler({AbonoExcesivoException.class, ObjetivoMenorQueLoAhorradoException.class})
    public ResponseEntity<ApiError> metaInvalida(RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    /**
     * 403: el usuario esta autenticado pero su rol no alcanza (RF-023).
     *
     * @PreAuthorize lanza AuthorizationDeniedException DENTRO del controlador, o sea
     * despues del filtro que atiende las denegaciones. Sin este manejador caia en el
     * cajon de "error inesperado" y respondia 500: le decia al usuario que el servidor
     * se rompio cuando lo que pasa es que no tiene permiso, y ademas ensuciaba el log
     * con un ERROR por cada intento normal de acceso.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accesoDenegado(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Acceso denegado a {} {}", req.getMethod(), req.getRequestURI());
        return build(HttpStatus.FORBIDDEN, "No tienes permiso para esta operacion", req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errores.put(fe.getField(), fe.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Error de validacion en los datos enviados", req, errores);
    }

    /**
     * Red de seguridad. Cualquier excepcion no prevista se registra completa en el
     * log y al cliente le llega un mensaje neutro.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> errorNoPrevisto(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error inesperado. Intente de nuevo mas tarde.", req, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String mensaje,
                                           HttpServletRequest req, Map<String, String> fieldErrors) {
        ApiError body = new ApiError(
                OffsetDateTime.now(), status.value(), status.getReasonPhrase(),
                mensaje, req.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
