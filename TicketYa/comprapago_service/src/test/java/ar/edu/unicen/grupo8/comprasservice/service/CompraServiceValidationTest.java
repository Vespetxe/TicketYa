package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.client.EventoClient;
import ar.edu.unicen.grupo8.comprasservice.client.ReservaClient;
import ar.edu.unicen.grupo8.comprasservice.dto.CrearCompraDTO;
import ar.edu.unicen.grupo8.comprasservice.dto.DetalleCompraDTO;
import ar.edu.unicen.grupo8.comprasservice.repository.CompraRepository;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import ar.edu.unicen.grupo8.comprasservice.repository.ControlLimiteEntradasRepository;

class CompraServiceValidacionTest {

    @Test
    void debeRechazarCompraInvalidaAntesDeEjecutarLaLogica() {


        CompraRepository compraRepository = mock(CompraRepository.class);
        ReservaClient reservaClient = mock(ReservaClient.class);
        EventoClient eventoClient = mock(EventoClient.class);

        ControlLimiteEntradasRepository controlLimiteEntradasRepository =
                mock(ControlLimiteEntradasRepository.class);

        CompraService servicio = new CompraService(
                compraRepository,
                reservaClient,
                eventoClient,
                controlLimiteEntradasRepository,
                6
        );


        ProxyFactory proxyFactory = new ProxyFactory(servicio);
        proxyFactory.addAdvice(new MethodValidationInterceptor());

        CompraService servicioValidado =
                (CompraService) proxyFactory.getProxy();

        DetalleCompraDTO detalle = new DetalleCompraDTO();
        detalle.setSectorId(1L);
        detalle.setCantidad(0);

        CrearCompraDTO dto = new CrearCompraDTO();
        dto.setClienteId(10L);
        dto.setEventoId(20L);
        dto.setReservaId(30L);
        dto.setDetalles(List.of(detalle));

        assertThrows(
                ConstraintViolationException.class,
                () -> servicioValidado.crearCompra(dto)
        );


        verifyNoInteractions(
                compraRepository,
                reservaClient,
                eventoClient,
                controlLimiteEntradasRepository
        );

    }
}
