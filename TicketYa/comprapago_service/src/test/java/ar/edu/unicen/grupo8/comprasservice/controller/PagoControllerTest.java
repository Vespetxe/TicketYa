
package ar.edu.unicen.grupo8.comprasservice.controller;

import ar.edu.unicen.grupo8.comprasservice.entity.enums.MedioPago;

import ar.edu.unicen.grupo8.comprasservice.entity.Pago;
import ar.edu.unicen.grupo8.comprasservice.service.PagoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import ar.edu.unicen.grupo8.comprasservice.dto.ResultadoProcesamientoPagoDTO;
import ar.edu.unicen.grupo8.comprasservice.dto.ProcesarPagoDTO;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import ar.edu.unicen.grupo8.comprasservice.exception.PagoNoEncontradoException;

class PagoControllerTest {

    private PagoService pagoService;
    private PagoController pagoController;

    @BeforeEach
    void setUp() {
        pagoService = mock(PagoService.class);
        pagoController = new PagoController(pagoService);
    }


    @Test
    void debeProcesarPagoCorrectamente() {

        Long compraId = 1L;
        String claveIdempotencia = "pago-compra-1";

        Pago pago = new Pago();

        ResultadoProcesamientoPagoDTO resultado =
                new ResultadoProcesamientoPagoDTO(pago, true);

        when(pagoService.procesarPagoConResultado(
                compraId, claveIdempotencia, MedioPago.TARJETA_DEBITO
        )).thenReturn(resultado);

        var dto = new ProcesarPagoDTO();
        dto.setCompraId(compraId);
        dto.setClaveIdempotencia(claveIdempotencia);
        dto.setMedioPago(MedioPago.TARJETA_DEBITO);

        ResponseEntity<ar.edu.unicen.grupo8.comprasservice.dto.PagoDTO> respuesta =
                pagoController.procesarPago(dto);

        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
        assertEquals(pago.getEstado(), respuesta.getBody().estado());

        verify(pagoService, times(1))
                .procesarPagoConResultado(compraId, claveIdempotencia, MedioPago.TARJETA_DEBITO);
    }




    @Test
    void debeRecibirSolicitudHttpParaProcesarPago() throws Exception {

        Long compraId = 1L;
        String claveIdempotencia = "pago-compra-1";

        Pago pago = new Pago();

        ResultadoProcesamientoPagoDTO resultado =
                new ResultadoProcesamientoPagoDTO(pago, true);

        when(pagoService.procesarPagoConResultado(
                compraId, claveIdempotencia, MedioPago.TARJETA_DEBITO
        )).thenReturn(resultado);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(pagoController)
                .build();

        String json = """
            {
                "compraId": 1,
                "claveIdempotencia": "pago-compra-1",
                "medioPago": "TARJETA_DEBITO"
            }
            """;

        mockMvc.perform(post("/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        verify(pagoService, times(1))
                .procesarPagoConResultado(compraId, claveIdempotencia, MedioPago.TARJETA_DEBITO);
    }



    @Test
    void debeRechazarSolicitudConDatosInvalidos() throws Exception {

        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(pagoController)
                .setValidator(validator)
                .build();

        String json = """
            {
                "compraId": -1,
                "claveIdempotencia": "",
                "medioPago": "TARJETA_DEBITO"
            }
            """;

        mockMvc.perform(post("/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(pagoService);

        validator.close();
    }



    @Test
    void debeDevolverConflictoCuandoLaCompraYaTieneUnPago() throws Exception {

        Long compraId = 1L;
        String claveIdempotencia = "pago-duplicado";

        when(pagoService.procesarPagoConResultado(
                compraId, claveIdempotencia, MedioPago.TARJETA_DEBITO
        )).thenThrow(new IllegalStateException(
                "La compra ya tiene un pago aprobado"
        ));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(pagoController)
                .setControllerAdvice(new PagoExceptionHandler())
                .build();

        String json = """
            {
                "compraId": 1,
                "claveIdempotencia": "pago-duplicado",
                "medioPago": "TARJETA_DEBITO"
            }
            """;

        mockMvc.perform(post("/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "La compra ya tiene un pago aprobado"
                ));

        verify(pagoService, times(1))
                .procesarPagoConResultado(compraId, claveIdempotencia, MedioPago.TARJETA_DEBITO);
    }




    @Test
    void debeDevolverBadRequestCuandoLaReservaEsInvalida() throws Exception {

        Long compraId = 1L;
        String claveIdempotencia = "pago-reserva-invalida";

        when(pagoService.procesarPagoConResultado(
                compraId, claveIdempotencia, MedioPago.TARJETA_DEBITO
        )).thenThrow(new IllegalArgumentException(
                "La reserva no es valida, esta vencida o no corresponde a la compra"
        ));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(pagoController)
                .setControllerAdvice(new PagoExceptionHandler())
                .build();

        String json = """
            {
                "compraId": 1,
                "claveIdempotencia": "pago-reserva-invalida",
                "medioPago": "TARJETA_DEBITO"
            }
            """;

        mockMvc.perform(post("/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "La reserva no es valida, esta vencida o no corresponde a la compra"
                ));

        verify(pagoService, times(1))
                .procesarPagoConResultado(compraId, claveIdempotencia, MedioPago.TARJETA_DEBITO);
    }


    @Test
    void debeDevolverOkCuandoElPagoYaExiste() throws Exception {

        Long compraId = 1L;
        String claveIdempotencia = "pago-existente";

        Pago pago = new Pago();

        ResultadoProcesamientoPagoDTO resultado =
                new ResultadoProcesamientoPagoDTO(pago, false);

        when(pagoService.procesarPagoConResultado(
                compraId, claveIdempotencia, MedioPago.TARJETA_DEBITO
        )).thenReturn(resultado);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(pagoController)
                .build();

        String json = """
            {
                "compraId": 1,
                "claveIdempotencia": "pago-existente",
                "medioPago": "TARJETA_DEBITO"
            }
            """;

        mockMvc.perform(post("/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(pagoService, times(1))
                .procesarPagoConResultado(compraId, claveIdempotencia, MedioPago.TARJETA_DEBITO);
    }


    @Test
    void debeConsultarPagoPorId() throws Exception {

        Long pagoId = 1L;

        Pago pago = new Pago();
        pago.setId(pagoId);

        when(pagoService.obtenerPagoPorId(pagoId))
                .thenReturn(pago);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(pagoController)
                .build();

        mockMvc.perform(get("/pagos/{id}", pagoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(pagoService, times(1))
                .obtenerPagoPorId(pagoId);
    }

    @Test
    void debeConsultarPagosPorCompra() throws Exception {

        Long compraId = 15L;

        Pago pago1 = new Pago();
        pago1.setId(1L);

        Pago pago2 = new Pago();
        pago2.setId(2L);

        List<Pago> pagos = List.of(pago1, pago2);

        when(pagoService.obtenerPagosPorCompra(compraId))
                .thenReturn(pagos);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(pagoController)
                .build();

        mockMvc.perform(get("/pagos")
                        .param("compraId", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(pagoService, times(1))
                .obtenerPagosPorCompra(compraId);
    }


    @Test
    void debeDevolverNotFoundCuandoElPagoNoExiste() throws Exception {

        Long pagoId = 999L;

        when(pagoService.obtenerPagoPorId(pagoId))
                .thenThrow(new PagoNoEncontradoException(
                        "No existe un pago con ID: " + pagoId
                ));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(pagoController)
                .setControllerAdvice(new PagoExceptionHandler())
                .build();

        mockMvc.perform(get("/pagos/{id}", pagoId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "No existe un pago con ID: 999"
                ));

        verify(pagoService, times(1))
                .obtenerPagoPorId(pagoId);
    }








}
