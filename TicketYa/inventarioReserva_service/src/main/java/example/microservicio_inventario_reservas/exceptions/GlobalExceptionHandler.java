package example.microservicio_inventario_reservas.exceptions;

import example.microservicio_inventario_reservas.dtos.MensajeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<MensajeResponse> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new MensajeResponse(ex.getMessage(), false));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<MensajeResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MensajeResponse(ex.getMessage(), false));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MensajeResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MensajeResponse(ex.getLocalizedMessage(), false));
    }

    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<MensajeResponse> handleInvalidRequest(Exception ex) {
        return ResponseEntity.badRequest().body(new MensajeResponse("Solicitud invalida", false));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<MensajeResponse> handleDuplicate(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new MensajeResponse("Conflicto con un registro existente", false));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MensajeResponse> handleInternal(Exception ex) {
        org.slf4j.LoggerFactory.getLogger(getClass()).error("Fallo interno de inventario", ex);
        return ResponseEntity.internalServerError().body(new MensajeResponse("Error interno", false));
    }
}
