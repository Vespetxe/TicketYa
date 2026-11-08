
package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.entity.enums.MedioPago;

import ar.edu.unicen.grupo8.comprasservice.dto.CrearCompraDTO;
import ar.edu.unicen.grupo8.comprasservice.dto.DetalleCompraDTO;
import ar.edu.unicen.grupo8.comprasservice.entity.Compra;
import ar.edu.unicen.grupo8.comprasservice.repository.CompraRepository;
import ar.edu.unicen.grupo8.comprasservice.repository.ControlLimiteEntradasRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import ar.edu.unicen.grupo8.comprasservice.exception.CompraNoEncontradaException;

import static org.junit.jupiter.api.Assertions.*;
import ar.edu.unicen.grupo8.comprasservice.repository.PagoRepository;
import ar.edu.unicen.grupo8.comprasservice.entity.Pago;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoPago;
import ar.edu.unicen.grupo8.comprasservice.client.ReservaClientSimulado;
import ar.edu.unicen.grupo8.comprasservice.client.EntradaClientSimulado;

@SpringBootTest
@ActiveProfiles("test")
class CompraIntegracionTest {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private ControlLimiteEntradasRepository controlLimiteEntradasRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private CompraService compraService;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private PagoService pagoService;

    @Autowired
    private ReservaClientSimulado reservaClientSimulado;

    @Autowired
    private EntradaClientSimulado entradaClientSimulado;


    @Autowired
    private PasarelaPagoSimuladaService pasarela;

    @BeforeEach
    void limpiarBaseDeDatos() {

        pasarela.reiniciar();
        reservaClientSimulado.reiniciarReservas();
        entradaClientSimulado.reiniciarEntradas();

        TransactionTemplate transaccion =
                new TransactionTemplate(transactionManager);

        transaccion.executeWithoutResult(status -> {
            pagoRepository.deleteAll();
            compraRepository.deleteAll();
            controlLimiteEntradasRepository.deleteAll();
        });
    }


    @Test
    void debeConectarConMySQL() {
        assertNotNull(compraRepository);
        assertNotNull(controlLimiteEntradasRepository);

        assertEquals(0, compraRepository.count());
        assertEquals(0, controlLimiteEntradasRepository.count());
    }

    @Test
    void debeCrearRegistroDeControlEnMySQL() {
        TransactionTemplate transaccion =
                new TransactionTemplate(transactionManager);

        boolean registroExiste = Boolean.TRUE.equals(
                transaccion.execute(status -> {
                    controlLimiteEntradasRepository.asegurarRegistro(10L, 20L);

                    return controlLimiteEntradasRepository
                            .buscarConBloqueo(10L, 20L)
                            .isPresent();
                })
        );

        assertTrue(registroExiste);
    }

    @Test
    void debeImpedirSuperarLimiteConComprasConcurrentes() throws Exception {

        // Dos compras de cuatro entradas para el mismo cliente y evento.
        CrearCompraDTO compra1 = crearDTO(100L, 200L, 301L, 4);
        CrearCompraDTO compra2 = crearDTO(100L, 200L, 302L, 4);

        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> resultado1 = ejecutor.submit(() -> {
                inicio.await();
                return intentarCompra(compra1);
            });

            Future<Boolean> resultado2 = ejecutor.submit(() -> {
                inicio.await();
                return intentarCompra(compra2);
            });

            // Liberar los dos hilos simultáneamente.
            inicio.countDown();

            boolean exito1 = resultado1.get(30, TimeUnit.SECONDS);
            boolean exito2 = resultado2.get(30, TimeUnit.SECONDS);

            // Exactamente una compra debe ser aceptada.
            assertNotEquals(exito1, exito2);

            // La base de datos debe contener una sola compra.
            var comprasGuardadas = compraRepository.findAll();

            assertEquals(1, comprasGuardadas.size());

            // Consultar los detalles dentro de una transacción.
            TransactionTemplate transaccion =
                    new TransactionTemplate(transactionManager);

            Integer cantidadEntradas = transaccion.execute(status -> {
                var compraGuardada = compraRepository.findById(
                        comprasGuardadas.get(0).getId()
                ).orElseThrow();

                return compraGuardada.getDetalles()
                        .stream()
                        .mapToInt(detalle -> detalle.getCantidad())
                        .sum();
            });

            assertNotNull(cantidadEntradas);
            assertEquals(4, cantidadEntradas);
            assertTrue(cantidadEntradas <= 6);

        } finally {
            ejecutor.shutdownNow();
        }
    }

    // TASK 3.5 - Consultar compra existente por ID
    @Test
    void debeConsultarCompraExistentePorIdEnMySQL() {

        Compra compraCreada = compraService.crearCompra(
                crearDTO(10L, 20L, 30L, 2)
        );

        Compra compraEncontrada = compraService.obtenerCompraPorId(
                compraCreada.getId()
        );

        assertNotNull(compraEncontrada);
        assertEquals(compraCreada.getId(), compraEncontrada.getId());
        assertEquals(10L, compraEncontrada.getClienteId());
        assertEquals(20L, compraEncontrada.getEventoId());
        assertEquals(30L, compraEncontrada.getReservaId());
    }


    @Test
    void debeRechazarConsultaDeCompraInexistenteEnMySQL() {

        assertThrows(
                CompraNoEncontradaException.class,
                () -> compraService.obtenerCompraPorId(Long.MAX_VALUE)
        );
    }


    @Test
    void debeConsultarComprasPorClienteEnMySQL() {

        // Dos compras del cliente 10.
        compraService.crearCompra(
                crearDTO(10L, 20L, 30L, 2)
        );

        compraService.crearCompra(
                crearDTO(10L, 21L, 31L, 1)
        );

        // Una compra de otro cliente.
        compraService.crearCompra(
                crearDTO(11L, 20L, 32L, 1)
        );

        List<Compra> comprasCliente10 =
                compraService.obtenerComprasPorCliente(10L);

        assertEquals(2, comprasCliente10.size());

        assertTrue(
                comprasCliente10.stream()
                        .allMatch(compra -> compra.getClienteId().equals(10L))
        );

        List<Compra> comprasCliente11 =
                compraService.obtenerComprasPorCliente(11L);

        assertEquals(1, comprasCliente11.size());

        // Cliente sin compras.
        List<Compra> comprasCliente999 =
                compraService.obtenerComprasPorCliente(999L);

        assertNotNull(comprasCliente999);
        assertTrue(comprasCliente999.isEmpty());
    }

    private boolean intentarCompra(CrearCompraDTO dto) {
        try {
            compraService.crearCompra(dto);
            return true;

        } catch (IllegalArgumentException excepcion) {
            if (!"La compra supera el limite de entradas permitido por cliente y evento"
                    .equals(excepcion.getMessage())) {
                throw excepcion;
            }

            return false;
        }
    }

    private CrearCompraDTO crearDTO(
            Long clienteId,
            Long eventoId,
            Long reservaId,
            int cantidad
    ) {
        DetalleCompraDTO detalle = new DetalleCompraDTO();
        detalle.setSectorId(1L);
        detalle.setCantidad(cantidad);

        CrearCompraDTO dto = new CrearCompraDTO();
        dto.setClienteId(clienteId);
        dto.setEventoId(eventoId);
        dto.setReservaId(reservaId);
        dto.setDetalles(List.of(detalle));

        return dto;
    }


    @Test
    void debeRegistrarPagoAprobadoEnMySQL() {

        // Creamos una compra con una reserva válida y dos entradas.
        Compra compra = compraService.crearCompra(
                crearDTO(10L, 20L, 30L, 2)
        );

        // Procesamos el pago.
        Pago pagoProcesado = pagoService.procesarPago(
                compra.getId(),
                "clave-integracion-123", MedioPago.TARJETA_DEBITO
        );

        // Consultamos los pagos guardados en MySQL.
        List<Pago> pagosGuardados =
                pagoRepository.findByCompraId(compra.getId());

        assertEquals(1, pagosGuardados.size());

        Pago pagoGuardado = pagosGuardados.get(0);

        // Verificamos el resultado y los datos persistidos.
        assertEquals(EstadoPago.APROBADO, pagoGuardado.getEstado());
        assertEquals(compra.getImporteTotal(), pagoGuardado.getImporte());
        assertEquals(
                "clave-integracion-123",
                pagoGuardado.getClaveIdempotencia()
        );

        assertNotNull(pagoGuardado.getFechaCreacion());
        assertNotNull(pagoGuardado.getFechaProcesamiento());
        assertNotNull(pagoGuardado.getReferenciaExterna());

        assertEquals(pagoProcesado.getId(), pagoGuardado.getId());

        // Verificamos que la reserva quedó confirmada.
        assertEquals(
                "CONFIRMADA",
                reservaClientSimulado.consultarReserva(30L).estado()
        );

        // Verificamos que se emitieron las dos entradas.
        var entradas = entradaClientSimulado.consultarEntradas(compra.getId());

        assertEquals(2, entradas.size());

        assertTrue(
                entradas.stream().allMatch(
                        entrada -> compra.getId().equals(entrada.compraId())
                )
        );

        assertTrue(
                entradas.stream().allMatch(
                        entrada -> "EMITIDA".equals(entrada.estado())
                )
        );
    }


    @Test
    void debeRegistrarPagoRechazadoEnMySQL() {

        // Creamos una compra válida.

        Compra compra = compraService.crearCompra(
                crearDTO(10L, 21L, 31L, 1)
        );


        // La pasarela simulada rechaza importes mayores a 100000.
        // Ajustamos el importe para probar ese escenario.
        compra.setImporteTotal(new BigDecimal("150000"));
        compraRepository.save(compra);

        // Procesamos el pago.
        Pago pagoProcesado = pagoService.procesarPago(
                compra.getId(),
                "clave-integracion-456", MedioPago.TARJETA_DEBITO
        );

        // Consultamos el resultado persistido en MySQL.
        List<Pago> pagosGuardados =
                pagoRepository.findByCompraId(compra.getId());

        assertEquals(1, pagosGuardados.size());

        Pago pagoGuardado = pagosGuardados.get(0);

        assertEquals(EstadoPago.RECHAZADO, pagoGuardado.getEstado());
        assertEquals(
                0,
                new BigDecimal("150000").compareTo(pagoGuardado.getImporte())
        );
        assertEquals(
                "clave-integracion-456",
                pagoGuardado.getClaveIdempotencia()
        );

        assertNotNull(pagoGuardado.getFechaCreacion());
        assertNotNull(pagoGuardado.getFechaProcesamiento());
        assertNotNull(pagoGuardado.getReferenciaExterna());

        assertEquals(pagoProcesado.getId(), pagoGuardado.getId());
    }


    @Test
    void debeImpedirPagosConcurrentesDeLaMismaCompra() throws Exception {

        // Creamos una compra válida.
        Compra compra = compraService.crearCompra(
                crearDTO(10L, 20L, 30L, 2)
        );

        Long compraId = compra.getId();

        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);

        try {
            // Primera solicitud de pago.
            Future<Boolean> resultado1 = ejecutor.submit(() -> {
                inicio.await();

                try {
                    pagoService.procesarPago(compraId, "clave-concurrente-1", MedioPago.TARJETA_DEBITO);
                    return true;
                } catch (IllegalStateException excepcion) {
                        return false;
                    } catch (IllegalArgumentException excepcion) {
                        if (!"La reserva no es valida, esta vencida o no corresponde a la compra"
                                .equals(excepcion.getMessage())) {
                            throw excepcion;
                        }
                        return false;
                    }
            });

            // Segunda solicitud de pago, con otra clave.
            Future<Boolean> resultado2 = ejecutor.submit(() -> {
                inicio.await();

                try {
                    pagoService.procesarPago(compraId, "clave-concurrente-2", MedioPago.TARJETA_DEBITO);
                    return true;
                } catch (IllegalStateException excepcion) {
                        return false;
                    } catch (IllegalArgumentException excepcion) {
                        if (!"La reserva no es valida, esta vencida o no corresponde a la compra"
                                .equals(excepcion.getMessage())) {
                            throw excepcion;
                        }
                        return false;
                    }
            });

            // Liberamos las dos solicitudes simultáneamente.
            inicio.countDown();

            boolean exito1 = resultado1.get(30, TimeUnit.SECONDS);
            boolean exito2 = resultado2.get(30, TimeUnit.SECONDS);

            // Exactamente una solicitud debe ser aceptada.
            assertNotEquals(exito1, exito2);

            // Comprobamos los pagos persistidos en MySQL.
            List<Pago> pagosGuardados =
                    pagoRepository.findByCompraId(compraId);

            assertEquals(1, pagosGuardados.size());

            assertEquals(
                    EstadoPago.APROBADO,
                    pagosGuardados.get(0).getEstado()
            );

        } finally {
            ejecutor.shutdownNow();
        }
    }


    @Test
    void debeRegistrarResultadoDesconocidoEnMySQL() {

        // Creamos una compra válida.
        Compra compra = compraService.crearCompra(
                crearDTO(10L, 20L, 30L, 2)
        );

        Long compraId = compra.getId();

        // La clave especial simula una falla de comunicación.
        Pago resultado = pagoService.procesarPago(
                compraId, "simular-timeout", MedioPago.TARJETA_DEBITO
        );

        // Verificamos que el pago quedó registrado.
        assertEquals(
                EstadoPago.RESULTADO_DESCONOCIDO,
                resultado.getEstado()
        );

        // Consultamos directamente los registros de MySQL.
        List<Pago> pagosGuardados =
                pagoRepository.findByCompraId(compraId);

        assertEquals(1, pagosGuardados.size());

        Pago pagoGuardado = pagosGuardados.get(0);

        assertEquals(
                EstadoPago.RESULTADO_DESCONOCIDO,
                pagoGuardado.getEstado()
        );

        assertNotNull(pagoGuardado.getFechaProcesamiento());
        assertNull(pagoGuardado.getReferenciaExterna());

        // Intentamos pagar nuevamente con otra clave.
        IllegalStateException excepcion = assertThrows(
                IllegalStateException.class,
                () -> pagoService.procesarPago(
                        compraId, "segundo-intento", MedioPago.TARJETA_DEBITO
        )
        );

        assertEquals(
                "La compra debe estar pendiente para procesar un pago",
                excepcion.getMessage()
        );

        // No debe haberse registrado otro pago.
        assertEquals(
                1,
                pagoRepository.findByCompraId(compraId).size()
        );
    }




}
