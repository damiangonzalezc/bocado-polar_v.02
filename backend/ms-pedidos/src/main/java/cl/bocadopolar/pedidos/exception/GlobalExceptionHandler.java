package cl.bocadopolar.pedidos.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/** Convierte cualquier excepción en un JSON {@link ApiError} con el código HTTP correcto. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ApiError> invalida(SolicitudInvalidaException ex, HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiError> reglaNegocio(ReglaNegocioException ex, HttpServletRequest req) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    @ExceptionHandler(NoAutorizadoException.class)
    public ResponseEntity<ApiError> noAutorizado(NoAutorizadoException ex, HttpServletRequest req) {
        return construir(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, null);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ApiError> denegado(AccesoDenegadoException ex, HttpServletRequest req) {
        return construir(HttpStatus.FORBIDDEN, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ServicioExternoException.class)
    public ResponseEntity<ApiError> externo(ServicioExternoException ex, HttpServletRequest req) {
        return construir(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> detalles = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> detalles.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> detalles.putIfAbsent(e.getObjectName(), e.getDefaultMessage()));
        return construir(HttpStatus.BAD_REQUEST, "Hay campos con errores", req, detalles);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> restriccion(ConstraintViolationException ex, HttpServletRequest req) {
        Map<String, String> detalles = new LinkedHashMap<>();
        ex.getConstraintViolations()
                .forEach(v -> detalles.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        return construir(HttpStatus.BAD_REQUEST, "Hay parámetros con errores", req, detalles);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingRequestHeaderException.class })
    public ResponseEntity<ApiError> malFormada(Exception ex, HttpServletRequest req) {
        String mensaje = "Solicitud mal formada: revisa el JSON, los tipos y los valores permitidos";
        if (ex instanceof MissingServletRequestParameterException m) {
            mensaje = "Falta el parámetro obligatorio '" + m.getParameterName() + "'";
        } else if (ex instanceof MissingRequestHeaderException h) {
            mensaje = "Falta el encabezado obligatorio '" + h.getHeaderName() + "'";
        } else if (ex instanceof MethodArgumentTypeMismatchException t) {
            mensaje = "Valor inválido para '" + t.getName() + "': " + t.getValue();
        }
        return construir(HttpStatus.BAD_REQUEST, mensaje, req, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> metodo(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return construir(HttpStatus.METHOD_NOT_ALLOWED, "Método " + ex.getMethod() + " no permitido en esta ruta", req, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> rutaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
        return construir(HttpStatus.NOT_FOUND, "La ruta no existe", req, null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> concurrencia(ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        return construir(HttpStatus.CONFLICT, "Otro usuario modificó este registro al mismo tiempo. Vuelve a intentarlo.", req, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integridad(DataIntegrityViolationException ex, HttpServletRequest req) {
        return construir(HttpStatus.CONFLICT, "El registro choca con otro existente (dato duplicado o en uso)", req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> general(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req, null);
    }

    private ResponseEntity<ApiError> construir(HttpStatus status, String mensaje, HttpServletRequest req,
            Map<String, String> detalles) {
        ApiError error = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), mensaje,
                req.getRequestURI(), detalles);
        return ResponseEntity.status(status).body(error);
    }
}
