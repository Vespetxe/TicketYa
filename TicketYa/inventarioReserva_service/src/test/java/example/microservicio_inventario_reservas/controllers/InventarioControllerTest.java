package example.microservicio_inventario_reservas.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import example.microservicio_inventario_reservas.dtos.*;
import example.microservicio_inventario_reservas.services.InventarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InventarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private InventarioService inventarioService;

    @Test
    @DisplayName("POST /eventos/{idEvento}/inventario - Debe retornar HTTP 201 Created")
    void inicializarInventario_Exito_HTTP201() throws Exception {
        Long idEvento = 100L;
        InicializarInventarioRequest request = new InicializarInventarioRequest(200L, false, 500, null);
        MensajeResponse response = new MensajeResponse("Inventario inicializado exitosamente", true);

        when(inventarioService.inicializarInventario(eq(idEvento), any(InicializarInventarioRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/eventos/{idEvento}/inventario", idEvento)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.mensaje").value("Inventario inicializado exitosamente"));
    }

    @Test
    @DisplayName("GET /disponibilidad/eventos/{idEvento}/sectores/{idSector} - Debe retornar HTTP 200 OK")
    void consultarDisponibilidad_Exito_HTTP200() throws Exception {
        Long idEvento = 100L;
        Long idSector = 200L;

        DetalleAsientoResponse asiento = new DetalleAsientoResponse(1L, "A", 10);
        DisponibilidadResponse response = new DisponibilidadResponse(
                idEvento, idSector, true, 100, 15, List.of(asiento)
        );

        when(inventarioService.consultarDisponibilidad(idEvento, idSector)).thenReturn(response);

        mockMvc.perform(get("/disponibilidad/eventos/{idEvento}/sectores/{idSector}", idEvento, idSector))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEvento").value(idEvento))
                .andExpect(jsonPath("$.idSector").value(idSector))
                .andExpect(jsonPath("$.esNumerado").value(true))
                .andExpect(jsonPath("$.disponibles").value(15))
                .andExpect(jsonPath("$.asientosLibres[0].idAsiento").value(1L));
    }

    @Test
    @DisplayName("GET /disponibilidad/eventos/{idEvento}/sectores/{idSector} - Debe retornar HTTP 404 Not Found si no existe")
    void consultarDisponibilidad_NoEncontrado_HTTP404() throws Exception {
        Long idEvento = 999L;
        Long idSector = 999L;

        when(inventarioService.consultarDisponibilidad(idEvento, idSector))
                .thenThrow(new example.microservicio_inventario_reservas.exceptions.RecursoNoEncontradoException("Inventario no encontrado"));

        mockMvc.perform(get("/disponibilidad/eventos/{idEvento}/sectores/{idSector}", idEvento, idSector))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exitoso").value(false))
                .andExpect(jsonPath("$.mensaje").value("Inventario no encontrado"));
    }

    @Test
    @DisplayName("POST /ocupaciones/liberar - Debe retornar HTTP 200 OK")
    void liberarPorDevolucion_Exito_HTTP200() throws Exception {
        LiberarDevolucionRequest request = new LiberarDevolucionRequest(100L, 200L, 2, List.of(5L, 6L));
        MensajeResponse response = new MensajeResponse("Lugares reintegrados al inventario exitosamente", true);

        when(inventarioService.liberarPorDevolucion(any(LiberarDevolucionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/ocupaciones/liberar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.mensaje").value("Lugares reintegrados al inventario exitosamente"));
    }

    @Test
    @DisplayName("POST /eventos/{idEvento}/cancelar-inventario - Debe retornar HTTP 200 OK")
    void cancelarInventarioEvento_Exito_HTTP200() throws Exception {
        Long idEvento = 100L;
        MensajeResponse response = new MensajeResponse("Inventario del evento invalidado correctamente por cancelación", true);

        when(inventarioService.cancelarInventarioEvento(idEvento)).thenReturn(response);

        mockMvc.perform(post("/eventos/{idEvento}/cancelar-inventario", idEvento))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.mensaje").value("Inventario del evento invalidado correctamente por cancelación"));
    }
}
