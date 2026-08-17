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
import com.finmind.obligaciones.service.ServicioObligaciones.NombreDeObligacionRepetidoException;
import com.finmind.obligaciones.service.ServicioObligaciones.TipoDeObligacionInvalidoException;
import com.finmind.obligaciones.service.ServicioObligaciones.ObligacionCerradaException;
import com.finmind.obligaciones.service.ServicioObligaciones.PagoExcedeLaDeudaException;
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
