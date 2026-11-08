package ar.edu.unicen.grupo8.comprasservice.exception;

import ar.edu.unicen.grupo8.comprasservice.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).findFirst().orElse("Solicitud invalida"));
    }
    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleSolicitud(Exception ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "La solicitud contiene datos invalidos o incompletos");
    }
    @ExceptionHandler(CompraNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handleCompraNoEncontrada(CompraNoEncontradaException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }
    @ExceptionHandler(PagoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handlePagoNoEncontrado(PagoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }
    @ExceptionHandler({DevolucionNoEncontradaException.class, ReintegroNoEncontradoException.class, EventoNoEncontradoException.class})
    public ResponseEntity<ErrorResponse> handleDevolucionNoEncontrada(RuntimeException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflicto(IllegalStateException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage());
    }
    @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
    public ResponseEntity<ErrorResponse> handlePersistencia(Exception ex) {
        return respuesta(HttpStatus.CONFLICT, "Conflicto con una operacion existente; consulte su resultado antes de reintentar");
    }
    @ExceptionHandler(ComunicacionPasarelaException.class)
    public ResponseEntity<ErrorResponse> handleComunicacion(ComunicacionPasarelaException ex) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar el servicio externo");
    }
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMetodo(Exception ex) {
        return respuesta(HttpStatus.METHOD_NOT_ALLOWED, "Metodo HTTP no permitido");
    }
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleContenido(Exception ex) {
        return respuesta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de contenido no admitido");
    }
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleFormatoRespuesta(Exception ex) {
        return respuesta(HttpStatus.NOT_ACCEPTABLE, "Formato de respuesta no admitido; los errores utilizan application/json");
    }
    @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class,
            org.springframework.web.servlet.NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleRuta(Exception ex) {
        return respuesta(HttpStatus.NOT_FOUND, "No existe el recurso solicitado");
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleErrorInterno(Exception ex) {
        log.error("Error interno al procesar la solicitud", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la solicitud por un error interno");
    }
    private ResponseEntity<ErrorResponse> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).contentType(MediaType.APPLICATION_JSON).body(new ErrorResponse(estado.value(), mensaje));
    }
}
