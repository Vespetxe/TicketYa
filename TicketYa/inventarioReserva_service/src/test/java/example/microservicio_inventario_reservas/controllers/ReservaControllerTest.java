package example.microservicio_inventario_reservas.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import example.microservicio_inventario_reservas.dtos.*;
import example.microservicio_inventario_reservas.enums.EstadoReserva;
import example.microservicio_inventario_reservas.services.ReservaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private ReservaService reservaService;

    @Test
    @DisplayName("POST /reservas - Debe crear reserva temporal y responder HTTP 201 Created")
    void crearReservaTemporal_Exito_HTTP201() throws Exception {
        CrearReservaRequest request = new CrearReservaRequest(10L, 100L, 200L, 2, null);
        ReservaResponse response = new ReservaResponse(
                50L, 10L, 100L, 200L, 2,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(10),
                EstadoReserva.PENDIENTE, null
        );

        when(reservaService.crearReservaTemporal(any(CrearReservaRequest.class))).thenReturn(response);

        mockMvc.perform(post("/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idReserva").value(50L))
                .andExpect(jsonPath("$.idCliente").value(10L))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("POST /reservas - Debe responder HTTP 409 Conflict si no hay capacidad o aforo suficiente")
    void crearReservaTemporal_SinCapacidad_HTTP409() throws Exception {
        CrearReservaRequest request = new CrearReservaRequest(10L, 100L, 200L, 10, null);

        when(reservaService.crearReservaTemporal(any(CrearReservaRequest.class)))
                .thenThrow(new IllegalStateException("Capacidad o aforo insuficiente"));

        mockMvc.perform(post("/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.exitoso").value(false))
                .andExpect(jsonPath("$.mensaje").value("Capacidad o aforo insuficiente"));
    }

    @Test
    @DisplayName("GET /reservas/{id} - Debe responder HTTP 200 OK con los datos de la reserva")
    void obtenerReservaPorId_Exito_HTTP200() throws Exception {
        Long idReserva = 50L;
        ReservaResponse response = new ReservaResponse(
                idReserva, 10L, 100L, 200L, 1,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(10),
                EstadoReserva.CONFIRMADA, null
        );

        when(reservaService.obtenerReservaPorId(idReserva)).thenReturn(response);

        mockMvc.perform(get("/reservas/{id}", idReserva))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva").value(idReserva))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    @DisplayName("GET /reservas?idCliente={idCliente} - Debe responder HTTP 200 OK con la lista de reservas del cliente")
    void obtenerReservasPorCliente_Exito_HTTP200() throws Exception {
        Long idCliente = 10L;
        ReservaResponse r1 = new ReservaResponse(50L, idCliente, 100L, 200L, 1, LocalDateTime.now(), LocalDateTime.now().plusMinutes(10), EstadoReserva.CONFIRMADA, null);
        ReservaResponse r2 = new ReservaResponse(51L, idCliente, 101L, 201L, 2, LocalDateTime.now(), LocalDateTime.now().plusMinutes(10), EstadoReserva.PENDIENTE, null);

        when(reservaService.obtenerReservasPorCliente(idCliente)).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/reservas").param("idCliente", String.valueOf(idCliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].idCliente").value(idCliente))
                .andExpect(jsonPath("$[1].idCliente").value(idCliente));
    }

    @Test
    @DisplayName("POST /reservas/{id}/confirmar - Debe responder HTTP 200 OK")
    void confirmarReserva_Exito_HTTP200() throws Exception {
        Long idReserva = 50L;
        MensajeResponse response = new MensajeResponse("Reserva confirmada exitosamente", true);

        when(reservaService.confirmarReserva(idReserva)).thenReturn(response);

        mockMvc.perform(post("/reservas/{id}/confirmar", idReserva))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.mensaje").value("Reserva confirmada exitosamente"));
    }

    @Test
    @DisplayName("POST /reservas/{id}/liberar - Debe responder HTTP 200 OK")
    void liberarReserva_Exito_HTTP200() throws Exception {
        Long idReserva = 50L;
        MensajeResponse response = new MensajeResponse("Reserva liberada y disponibilidad restablecida", true);

        when(reservaService.liberarReserva(idReserva)).thenReturn(response);

        mockMvc.perform(post("/reservas/{id}/liberar", idReserva))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.mensaje").value("Reserva liberada y disponibilidad restablecida"));
    }
}