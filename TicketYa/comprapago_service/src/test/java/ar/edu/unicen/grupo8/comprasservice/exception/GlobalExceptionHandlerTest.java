
package ar.edu.unicen.grupo8.comprasservice.exception;

import ar.edu.unicen.grupo8.comprasservice.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    @Test
    void debeResponderBadRequestAnteArgumentoInvalido() {

        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        IllegalArgumentException exception =
                new IllegalArgumentException("Cantidad invalida");

        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalArgument(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Cantidad invalida", response.getBody().message());
    }
}
