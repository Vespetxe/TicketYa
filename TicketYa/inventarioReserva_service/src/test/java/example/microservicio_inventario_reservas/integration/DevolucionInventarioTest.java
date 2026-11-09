package example.microservicio_inventario_reservas.integration;

import example.microservicio_inventario_reservas.dtos.*;
import example.microservicio_inventario_reservas.repositories.*;
import example.microservicio_inventario_reservas.services.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DevolucionInventarioTest {
    @Autowired InventarioService inventarios;
    @Autowired ReservaService reservas;
    @Autowired DevolucionInventarioService devoluciones;
    @Autowired LugarDevueltoRepository journal;
    @Autowired ReservaRepository reservaRepository;
    @Autowired InventarioSectorRepository stock;
    private long evento;
    @BeforeEach void preparar() {evento=ThreadLocalRandom.current().nextLong(1,Long.MAX_VALUE);}
    @AfterEach void limpiar() {journal.deleteAll();reservaRepository.deleteAll();stock.deleteAll();}
    private long reserva(boolean numerada) {
        inventarios.inicializarInventario(evento,new InicializarInventarioRequest(evento,numerada,2,
                numerada?List.of(new AsientoDTO(101L,"A",1),new AsientoDTO(102L,"A",2)):null));
        inventarios.publicar(evento);
        var asientos=inventarios.consultarDisponibilidad(evento,evento).getAsientosLibres();
        long id=reservas.crearReservaTemporal(new CrearReservaRequest(1L,evento,evento,2,
                numerada?asientos.stream().map(DetalleAsientoResponse::getIdAsiento).toList():null)).getIdReserva();
        reservas.confirmarReserva(id);return id;
    }
    private DevolverLugaresRequest seleccion(String ticket,Long asiento) {
        return new DevolverLugaresRequest(List.of(new DevolverLugaresRequest.Unidad(ticket,asiento)));
    }
    @Test void parcialGeneralEsDurableYNoDuplicaStock() {
        long id=reserva(false);var req=seleccion("ticket-a",null);
        devoluciones.devolver(id,req);devoluciones.devolver(id,req);
        assertEquals(List.of("ticket-a"),devoluciones.consultar(id));
        assertEquals(1,inventarios.consultarDisponibilidad(evento,evento).getDisponibles());
        devoluciones.devolver(id,seleccion("ticket-b",null));
        assertThrows(IllegalStateException.class,()->devoluciones.devolver(id,seleccion("ticket-c",null)));
        assertEquals(2,inventarios.consultarDisponibilidad(evento,evento).getDisponibles());
        assertThrows(IllegalStateException.class,()->reservas.compensarReserva(id));
    }
    @Test void numeradaRechazaAsientoAjenoYAsientoDevueltoConOtraIdentidad() {
        long id=reserva(true);Long asiento=reservas.obtenerReservaPorId(id).getAsientosReservados().get(0).getIdAsiento();
        assertThrows(IllegalArgumentException.class,()->devoluciones.devolver(id,seleccion("ajena",Long.MAX_VALUE)));
        devoluciones.devolver(id,seleccion("ticket-a",asiento));
        assertThrows(IllegalStateException.class,()->devoluciones.devolver(id,seleccion("ticket-b",asiento)));
        assertEquals(1,inventarios.consultarDisponibilidad(evento,evento).getDisponibles());
    }
    @Test void compensacionConfirmadaEsIdempotente() {
        long id=reserva(true);reservas.compensarReserva(id);reservas.compensarReserva(id);
        assertEquals(2,inventarios.consultarDisponibilidad(evento,evento).getDisponibles());
        assertThrows(IllegalStateException.class,()->reservas.confirmarReserva(id));
    }
    @Test void devolucionCanceladaNoReofreceStock() {
        long id=reserva(false);inventarios.cancelarInventarioEvento(evento);
        devoluciones.devolver(id,seleccion("ticket-a",null));
        assertEquals(0,inventarios.consultarDisponibilidad(evento,evento).getDisponibles());
    }
    @Test void concurrenciaDeLaMismaEntradaReponeUnaVez() throws Exception {
        long id=reserva(false);ExecutorService executor=Executors.newFixedThreadPool(6);CountDownLatch inicio=new CountDownLatch(1);
        try {
            List<Future<?>> trabajos=new ArrayList<>();
            for(int n=0;n<6;n++) trabajos.add(executor.submit(()->{inicio.await();devoluciones.devolver(id,seleccion("ticket-a",null));return null;}));
            inicio.countDown();for(var trabajo:trabajos)trabajo.get(20,TimeUnit.SECONDS);
            assertEquals(1,inventarios.consultarDisponibilidad(evento,evento).getDisponibles());
        } finally {executor.shutdownNow();}
    }
    @Test void inicializarRepetidoConservaReferenciasDeCatalogo() {
        var req=new InicializarInventarioRequest(evento,true,1,List.of(new AsientoDTO(901L,"A",1)));
        inventarios.inicializarInventario(evento,req);inventarios.inicializarInventario(evento,req);
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(tx->{
            var i=stock.findByIdEventoAndIdSector(evento,evento).orElseThrow();
            assertEquals(901L,i.getAsientos().get(0).getCatalogoAsientoId());
            assertEquals(1,i.getAsientos().size());
        });
    }
    @Test void cancelacionPreviaImpideInicializacionDemorada() {
        inventarios.cancelarInventarioEvento(evento);
        assertThrows(IllegalStateException.class,()->inventarios.inicializarInventario(evento,
                new InicializarInventarioRequest(evento,false,2,null)));
    }
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
}
