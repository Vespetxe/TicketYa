
package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.client.EventoClient;
import ar.edu.unicen.grupo8.comprasservice.client.ReservaClient;
import ar.edu.unicen.grupo8.comprasservice.dto.CrearCompraDTO;
import ar.edu.unicen.grupo8.comprasservice.dto.DetalleCompraDTO;
import ar.edu.unicen.grupo8.comprasservice.entity.Compra;
import ar.edu.unicen.grupo8.comprasservice.entity.ControlLimiteEntradas;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoCompra;
import ar.edu.unicen.grupo8.comprasservice.repository.CompraRepository;
import ar.edu.unicen.grupo8.comprasservice.repository.ControlLimiteEntradasRepository;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import ar.edu.unicen.grupo8.comprasservice.exception.CompraNoEncontradaException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private ReservaClient reservaClient;

    @Mock
    private EventoClient eventoClient;

    @Mock
    private ControlLimiteEntradasRepository controlLimiteEntradasRepository;

    private CompraService compraService;
    private CrearCompraDTO compraDTO;

    private static final List<EstadoCompra> ESTADOS_ACTIVOS = List.of(
            EstadoCompra.PENDIENTE,
            EstadoCompra.PROCESANDO,
            EstadoCompra.COMPLETADA,
            EstadoCompra.DEVUELTA_PARCIAL
    );

    @BeforeEach
    void configurar() {
        compraService = new CompraService(
                compraRepository,
                reservaClient,
                eventoClient,
                controlLimiteEntradasRepository,
                6
        );

        lenient().when(
                controlLimiteEntradasRepository.buscarConBloqueo(
                        anyLong(), anyLong()
                )
        ).thenReturn(
                Optional.of(new ControlLimiteEntradas(10L, 20L))
        );

        DetalleCompraDTO detalle = new DetalleCompraDTO();
        detalle.setSectorId(1L);
        detalle.setCantidad(2);

        compraDTO = new CrearCompraDTO();
        compraDTO.setClienteId(10L);
        compraDTO.setEventoId(20L);
        compraDTO.setReservaId(30L);
        compraDTO.setDetalles(List.of(detalle));
        lenient().when(reservaClient.consultarReserva(anyLong())).thenAnswer(i ->
                new ar.edu.unicen.grupo8.comprasservice.dto.ReservaDTO(compraDTO.getReservaId(), compraDTO.getClienteId(),
                        compraDTO.getEventoId(), "TEMPORAL", java.time.LocalDateTime.now(java.time.Clock.systemUTC()).plusMinutes(15),
                        compraDTO.getDetalles().stream().map(d -> new ar.edu.unicen.grupo8.comprasservice.dto.DetalleEmisionDTO(
                                d.getSectorId(), d.getAsientoId(), d.getCantidad())).toList()));
        lenient().when(eventoClient.obtenerPrecioSector(anyLong(), anyLong())).thenReturn(new BigDecimal("15000.00"));
    }

    @Test
    void debeCrearCompraPendienteCorrectamente() {
        when(eventoClient.eventoValido(20L)).thenReturn(true);
        when(reservaClient.reservaValida(30L, 10L, 20L))
                .thenReturn(true);

        when(eventoClient.obtenerPrecioSector(20L, 1L))
                .thenReturn(new BigDecimal("15000.00"));

        when(compraRepository.save(any(Compra.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Compra compra = compraService.crearCompra(compraDTO);

        assertNotNull(compra);
        assertEquals(EstadoCompra.PENDIENTE, compra.getEstado());
        assertEquals(10L, compra.getClienteId());
        assertEquals(20L, compra.getEventoId());
        assertEquals(30L, compra.getReservaId());

        assertEquals(
                0,
                new BigDecimal("30000.00")
                        .compareTo(compra.getImporteTotal())
        );

        assertEquals(1, compra.getDetalles().size());
        assertSame(compra, compra.getDetalles().get(0).getCompra());

        assertEquals(
                0,
                new BigDecimal("15000.00").compareTo(
                        compra.getDetalles().get(0).getPrecioUnitario()
                )
        );

        assertEquals(
                0,
                new BigDecimal("30000.00").compareTo(
                        compra.getDetalles().get(0).getSubtotal()
                )
        );

        verify(eventoClient).obtenerPrecioSector(20L, 1L);
        verify(compraRepository).save(any(Compra.class));
    }

    @Test
    void debeRechazarEventoInvalido() {
        when(eventoClient.eventoValido(20L)).thenReturn(false);

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> compraService.crearCompra(compraDTO)
        );

        assertEquals(
                "El evento indicado no es valido para realizar una compra",
                excepcion.getMessage()
        );

        verifyNoInteractions(reservaClient);
        verify(compraRepository, never()).save(any(Compra.class));
    }

    @Test
    void debeRechazarReservaInvalida() {
        when(eventoClient.eventoValido(20L)).thenReturn(true);
        when(reservaClient.reservaValida(30L, 10L, 20L))
                .thenReturn(false);

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> compraService.crearCompra(compraDTO)
        );

        assertEquals(
                "La reserva no es valida para el cliente y evento indicados",
                excepcion.getMessage()
        );

        verify(compraRepository, never()).save(any(Compra.class));
    }

    @Test
    void debeRechazarCompraConCantidadInvalida() {
        compraDTO.getDetalles().get(0).setCantidad(0);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            var errores = validator.validate(compraDTO);

            assertFalse(errores.isEmpty());

            assertTrue(
                    errores.stream().anyMatch(
                            error -> error.getPropertyPath()
                                    .toString()
                                    .contains("cantidad")
                    )
            );
        }

        verifyNoInteractions(
                eventoClient,
                reservaClient,
                compraRepository,
                controlLimiteEntradasRepository
        );
    }

    @Test
    void debeCalcularImporteTotalConVariosSectores() {
        DetalleCompraDTO detalle1 = new DetalleCompraDTO();
        detalle1.setSectorId(1L);
        detalle1.setCantidad(2);

        DetalleCompraDTO detalle2 = new DetalleCompraDTO();
        detalle2.setSectorId(2L);
        detalle2.setCantidad(3);

        compraDTO.setDetalles(List.of(detalle1, detalle2));

        when(eventoClient.eventoValido(20L)).thenReturn(true);
        when(reservaClient.reservaValida(30L, 10L, 20L))
                .thenReturn(true);

        when(eventoClient.obtenerPrecioSector(20L, 1L))
                .thenReturn(new BigDecimal("15000.00"));

        when(eventoClient.obtenerPrecioSector(20L, 2L))
                .thenReturn(new BigDecimal("25000.00"));

        when(compraRepository.save(any(Compra.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Compra compra = compraService.crearCompra(compraDTO);

        assertEquals(2, compra.getDetalles().size());

        assertEquals(
                0,
                new BigDecimal("30000.00").compareTo(
                        compra.getDetalles().get(0).getSubtotal()
                )
        );

        assertEquals(
                0,
                new BigDecimal("75000.00").compareTo(
                        compra.getDetalles().get(1).getSubtotal()
                )
        );

        assertEquals(
                0,
                new BigDecimal("105000.00").compareTo(
                        compra.getImporteTotal()
                )
        );

        verify(eventoClient).obtenerPrecioSector(20L, 1L);
        verify(eventoClient).obtenerPrecioSector(20L, 2L);
        verify(compraRepository).save(any(Compra.class));
    }

    @Test
    void debeRechazarPrecioCero() {
        when(eventoClient.eventoValido(20L)).thenReturn(true);
        when(reservaClient.reservaValida(30L, 10L, 20L))
                .thenReturn(true);

        when(eventoClient.obtenerPrecioSector(20L, 1L))
                .thenReturn(BigDecimal.ZERO);

        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.crearCompra(compraDTO)
        );

        verify(compraRepository, never()).save(any(Compra.class));
    }

    @Test
    void debeRechazarPrecioNegativo() {
        when(eventoClient.eventoValido(20L)).thenReturn(true);
        when(reservaClient.reservaValida(30L, 10L, 20L))
                .thenReturn(true);

        when(eventoClient.obtenerPrecioSector(20L, 1L))
                .thenReturn(new BigDecimal("-5000.00"));

        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.crearCompra(compraDTO)
        );

        verify(compraRepository, never()).save(any(Compra.class));
    }

    @Test
    void debeRechazarPrecioNulo() {
        when(eventoClient.eventoValido(20L)).thenReturn(true);
        when(reservaClient.reservaValida(30L, 10L, 20L))
                .thenReturn(true);

        when(eventoClient.obtenerPrecioSector(20L, 1L))
                .thenReturn(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.crearCompra(compraDTO)
        );

        verify(compraRepository, never()).save(any(Compra.class));
    }

    @Test
    void debeRechazarCompraQueSuperaLimiteDeEntradas() {
        when(eventoClient.eventoValido(20L)).thenReturn(true);
        when(reservaClient.reservaValida(30L, 10L, 20L))
                .thenReturn(true);

        when(compraRepository.contarEntradasPorClienteYEvento(
                10L, 20L, ESTADOS_ACTIVOS
        )).thenReturn(5L);

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> compraService.crearCompra(compraDTO)
        );

        assertEquals(
                "La compra supera el limite de entradas permitido por cliente y evento",
                excepcion.getMessage()
        );

        verify(compraRepository, never()).save(any(Compra.class));
        verify(eventoClient)
                .obtenerPrecioSector(anyLong(), anyLong());
    }

    @Test
    void debePermitirCompraQueAlcanzaExactamenteElLimite() {
        when(eventoClient.eventoValido(20L)).thenReturn(true);
        when(reservaClient.reservaValida(30L, 10L, 20L))
                .thenReturn(true);

        when(compraRepository.contarEntradasPorClienteYEvento(
                10L, 20L, ESTADOS_ACTIVOS
        )).thenReturn(4L);

        when(eventoClient.obtenerPrecioSector(20L, 1L))
                .thenReturn(new BigDecimal("15000.00"));

        when(compraRepository.save(any(Compra.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Compra compra = compraService.crearCompra(compraDTO);

        assertNotNull(compra);
        assertEquals(EstadoCompra.PENDIENTE, compra.getEstado());

        assertEquals(
                0,
                new BigDecimal("30000.00")
                        .compareTo(compra.getImporteTotal())
        );

        verify(compraRepository).save(any(Compra.class));
    }

    @Test
    void debeRespetarLimiteConfigurable() {
        CompraService servicioConLimite4 = new CompraService(
                compraRepository,
                reservaClient,
                eventoClient,
                controlLimiteEntradasRepository,
                4
        );

        when(eventoClient.eventoValido(20L)).thenReturn(true);
        when(reservaClient.reservaValida(30L, 10L, 20L))
                .thenReturn(true);

        when(compraRepository.contarEntradasPorClienteYEvento(
                10L, 20L, ESTADOS_ACTIVOS
        )).thenReturn(3L);

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> servicioConLimite4.crearCompra(compraDTO)
        );

        assertEquals(
                "La compra supera el limite de entradas permitido por cliente y evento",
                excepcion.getMessage()
        );

        verify(compraRepository, never()).save(any(Compra.class));
    }

    @Test
    void debeBloquearAntesDeContarEntradas() {
        when(eventoClient.eventoValido(20L)).thenReturn(true);
        when(reservaClient.reservaValida(30L, 10L, 20L))
                .thenReturn(true);

        when(compraRepository.contarEntradasPorClienteYEvento(
                10L, 20L, ESTADOS_ACTIVOS
        )).thenReturn(5L);

        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.crearCompra(compraDTO)
        );

        InOrder orden = inOrder(
                controlLimiteEntradasRepository,
                compraRepository
        );

        orden.verify(controlLimiteEntradasRepository)
                .asegurarRegistro(10L, 20L);

        orden.verify(controlLimiteEntradasRepository)
                .buscarConBloqueo(10L, 20L);

        orden.verify(compraRepository)
                .contarEntradasPorClienteYEvento(
                        10L, 20L, ESTADOS_ACTIVOS
                );
    }

    // TASK 3.5 - Consultas de compras

    @Test
    void debeObtenerCompraPorIdExistente() {
        Compra compra = new Compra();
        compra.setClienteId(10L);
        compra.setEventoId(20L);
        compra.setReservaId(30L);

        when(compraRepository.findById(1L))
                .thenReturn(Optional.of(compra));

        Compra resultado = compraService.obtenerCompraPorId(1L);

        assertSame(compra, resultado);
        assertEquals(10L, resultado.getClienteId());

        verify(compraRepository).findById(1L);
    }


    @Test
    void debeRechazarConsultaDeCompraInexistente() {
        when(compraRepository.findById(999L))
                .thenReturn(Optional.empty());

        CompraNoEncontradaException excepcion = assertThrows(
                CompraNoEncontradaException.class,
                () -> compraService.obtenerCompraPorId(999L)
        );

        assertEquals(
                "No existe una compra con ID: 999",
                excepcion.getMessage()
        );

        verify(compraRepository).findById(999L);
    }


    @Test
    void debeObtenerComprasPorCliente() {
        Compra compra1 = new Compra();
        compra1.setClienteId(10L);

        Compra compra2 = new Compra();
        compra2.setClienteId(10L);

        when(compraRepository.findByClienteId(10L))
                .thenReturn(List.of(compra1, compra2));

        List<Compra> resultado =
                compraService.obtenerComprasPorCliente(10L);

        assertEquals(2, resultado.size());
        assertSame(compra1, resultado.get(0));
        assertSame(compra2, resultado.get(1));

        verify(compraRepository).findByClienteId(10L);
    }

    @Test
    void debeDevolverListaVaciaSiClienteNoTieneCompras() {
        when(compraRepository.findByClienteId(999L))
                .thenReturn(List.of());

        List<Compra> resultado =
                compraService.obtenerComprasPorCliente(999L);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());

        verify(compraRepository).findByClienteId(999L);
    }


    @Test
    void debeRechazarCompraConReservaVencida() {

        // Configuramos una reserva vencida.
        compraDTO.setReservaId(2L);

        when(eventoClient.eventoValido(20L))
                .thenReturn(true);

        when(reservaClient.reservaValida(2L, 10L, 20L))
                .thenReturn(false);

        // Intentamos crear la compra.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> compraService.crearCompra(compraDTO)
        );

        assertEquals(
                "La reserva no es valida para el cliente y evento indicados",
                excepcion.getMessage()
        );

        // Verificamos que no se haya guardado.
        verify(compraRepository, never())
                .save(any(Compra.class));

        // Tampoco debe comenzar el control de entradas.
        verifyNoInteractions(controlLimiteEntradasRepository);
    }


    @Test
    void debeRechazarCompraConReservaDeOtroCliente() {

        // La compra pertenece al cliente 99.
        compraDTO.setClienteId(99L);
        compraDTO.setReservaId(1L);

        when(eventoClient.eventoValido(20L))
                .thenReturn(true);

        // El cliente de la compra no coincide
        // con el titular de la reserva.
        when(reservaClient.reservaValida(1L, 99L, 20L))
                .thenReturn(false);

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> compraService.crearCompra(compraDTO)
        );

        assertEquals(
                "La reserva no es valida para el cliente y evento indicados",
                excepcion.getMessage()
        );

        verify(reservaClient).reservaValida(1L, 99L, 20L);

        verify(compraRepository, never())
                .save(any(Compra.class));

        verifyNoInteractions(controlLimiteEntradasRepository);
    }


    @Test
    void debeRechazarCompraConReservaDeOtroEvento() {

        // La compra corresponde al evento 99.
        compraDTO.setEventoId(99L);
        compraDTO.setReservaId(1L);

        when(eventoClient.eventoValido(99L))
                .thenReturn(true);

        // La reserva no corresponde al evento solicitado.
        when(reservaClient.reservaValida(1L, 10L, 99L))
                .thenReturn(false);

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> compraService.crearCompra(compraDTO)
        );

        assertEquals(
                "La reserva no es valida para el cliente y evento indicados",
                excepcion.getMessage()
        );

        verify(reservaClient).reservaValida(1L, 10L, 99L);

        // No debe persistirse ninguna compra.
        verify(compraRepository, never())
                .save(any(Compra.class));

        // No debe iniciarse el control de entradas.
        verifyNoInteractions(controlLimiteEntradasRepository);
    }



}
