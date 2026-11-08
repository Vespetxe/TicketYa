package ar.edu.tudai.ticketya.servicio_entradas.api;
import ar.edu.tudai.ticketya.servicio_entradas.domain.InvalidOrderException;
import ar.edu.tudai.ticketya.servicio_entradas.domain.InvalidTicketStatusException;
import ar.edu.tudai.ticketya.servicio_entradas.domain.TicketNotFoundException;
import ar.edu.tudai.ticketya.servicio_entradas.integration.OrderNotConfirmedException;
import ar.edu.tudai.ticketya.servicio_entradas.integration.OrderNotFoundException;
import ar.edu.tudai.ticketya.servicio_entradas.integration.OrdersUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public class TicketApiExceptionHandler {
    @ExceptionHandler(TicketNotFoundException.class)
    ResponseEntity<ApiError> ticketNotFound(TicketNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND", exception.getMessage());
    }
    @ExceptionHandler(InvalidTicketStatusException.class)
    ResponseEntity<ApiError> invalidTicketStatus(InvalidTicketStatusException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_TICKET_STATUS", exception.getMessage());
    }
    @ExceptionHandler(InvalidTicketQueryException.class)
    ResponseEntity<ApiError> invalidTicketQuery(InvalidTicketQueryException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_TICKET_QUERY", exception.getMessage());
    }
    @ExceptionHandler(OrderNotFoundException.class)
    ResponseEntity<ApiError> orderNotFound(OrderNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", exception.getMessage());
    }
    @ExceptionHandler(OrderNotConfirmedException.class)
    ResponseEntity<ApiError> orderNotConfirmed(OrderNotConfirmedException exception) {
        return error(HttpStatus.CONFLICT, "ORDER_NOT_CONFIRMED", exception.getMessage());
    }
    @ExceptionHandler(InvalidOrderException.class)
    ResponseEntity<ApiError> invalidOrder(InvalidOrderException exception) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_ORDER", exception.getMessage());
    }
    @ExceptionHandler(OrdersUnavailableException.class)
    ResponseEntity<ApiError> ordersUnavailable(OrdersUnavailableException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "ORDERS_UNAVAILABLE", exception.getMessage());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidRequest(MethodArgumentNotValidException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "orderId is required");
    }
    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }
}
