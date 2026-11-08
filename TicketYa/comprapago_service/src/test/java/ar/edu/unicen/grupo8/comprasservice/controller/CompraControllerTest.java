
package ar.edu.unicen.grupo8.comprasservice.controller;

import ar.edu.unicen.grupo8.comprasservice.dto.ErrorResponse;
import ar.edu.unicen.grupo8.comprasservice.entity.Compra;
import ar.edu.unicen.grupo8.comprasservice.exception.CompraNoEncontradaException;
import ar.edu.unicen.grupo8.comprasservice.exception.GlobalExceptionHandler;
import ar.edu.unicen.grupo8.comprasservice.service.CompraService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CompraControllerTest {

    private MockMvc mockMvc;
    private CompraService compraService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void configurar() {
        compraService = Mockito.mock(CompraService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new CompraController(compraService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    void debeCrearCompraYDevolver201() throws Exception {
        Compra compra = new Compra();
        compra.setClienteId(10L);
        compra.setEventoId(20L);
        compra.setReservaId(30L);

        when(compraService.crearCompra(any()))
                .thenReturn(compra);

        String json = """
                {
                  "clienteId": 10,
                  "eventoId": 20,
                  "reservaId": 30,
                  "detalles": [
                    {
                      "sectorId": 1,
                      "cantidad": 2
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteId").value(10))
                .andExpect(jsonPath("$.eventoId").value(20))
                .andExpect(jsonPath("$.reservaId").value(30));

        verify(compraService).crearCompra(any());
    }

    @Test
    void debeConsultarCompraPorIdYDevolver200() throws Exception {
        Compra compra = new Compra();
        compra.setClienteId(10L);
        compra.setEventoId(20L);
        compra.setReservaId(30L);

        when(compraService.obtenerCompraPorId(1L))
                .thenReturn(compra);

        mockMvc.perform(get("/compras/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteId").value(10))
                .andExpect(jsonPath("$.eventoId").value(20));

        verify(compraService).obtenerCompraPorId(1L);
    }

    @Test
    void debeConsultarComprasPorClienteYDevolver200() throws Exception {
        Compra compra1 = new Compra();
        compra1.setClienteId(10L);

        Compra compra2 = new Compra();
        compra2.setClienteId(10L);

        when(compraService.obtenerComprasPorCliente(10L))
                .thenReturn(List.of(compra1, compra2));

        mockMvc.perform(get("/compras")
                        .param("clienteId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].clienteId").value(10))
                .andExpect(jsonPath("$[1].clienteId").value(10));

        verify(compraService).obtenerComprasPorCliente(10L);
    }

    @Test
    void debeDevolver404SiCompraNoExiste() throws Exception {
        when(compraService.obtenerCompraPorId(999L))
                .thenThrow(new CompraNoEncontradaException(999L));

        mockMvc.perform(get("/compras/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("No existe una compra con ID: 999"));
    }

    @Test
    void debeDevolver400SiLaCompraEsInvalida() throws Exception {
        when(compraService.crearCompra(any()))
                .thenThrow(new IllegalArgumentException(
                        "La reserva no es valida para el cliente y evento indicados"
                ));

        String json = """
                {
                  "clienteId": 10,
                  "eventoId": 20,
                  "reservaId": 30,
                  "detalles": [
                    {
                      "sectorId": 1,
                      "cantidad": 2
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("La reserva no es valida para el cliente y evento indicados"));
    }


    @Test
    void debeDevolver400SiFaltanDatosObligatorios() throws Exception {

        String json = """
            {
              "clienteId": 10,
              "eventoId": 20,
              "reservaId": 30,
              "detalles": [
                {
                  "sectorId": 1,
                  "cantidad": 0
                }
              ]
            }
            """;

        mockMvc.perform(post("/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(compraService);
    }

}
