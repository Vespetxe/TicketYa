package example.microservicio_inventario_reservas.integration;

import example.microservicio_inventario_reservas.dtos.*;
import example.microservicio_inventario_reservas.services.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PublicacionInventarioTest {
    @Autowired InventarioService inventarios;
    @Autowired ReservaService reservas;
    private long evento(){return ThreadLocalRandom.current().nextLong(1,Long.MAX_VALUE);}
    private InicializarInventarioRequest sector(long id){return new InicializarInventarioRequest(id,false,2,null);}
    private CrearReservaRequest reserva(long id,long sector){return new CrearReservaRequest(1L,id,sector,1,null);}

    @Test void publicacionInterrumpidaNoPermiteReservarNingunSectorYSeReanudaSinDuplicar() {
        long id=evento();inventarios.inicializarInventario(id,sector(10));
        assertThrows(IllegalStateException.class,()->reservas.crearReservaTemporal(reserva(id,10)));
        // Segundo sector nunca inicializado por la interrupcion.
        inventarios.inicializarInventario(id,sector(10));inventarios.inicializarInventario(id,sector(11));
        assertThrows(IllegalStateException.class,()->reservas.crearReservaTemporal(reserva(id,10)));
        assertThrows(IllegalStateException.class,()->reservas.crearReservaTemporal(reserva(id,11)));
        inventarios.publicar(id);inventarios.publicar(id);
        reservas.crearReservaTemporal(reserva(id,10));reservas.crearReservaTemporal(reserva(id,11));
        assertEquals(1,inventarios.consultarDisponibilidad(id,10L).getDisponibles());
        assertEquals(1,inventarios.consultarDisponibilidad(id,11L).getDisponibles());
    }
    @Test void publicacionTardiaNoReabreSuspensionNiCancelacion() {
        for(boolean cancelado:new boolean[]{false,true}) {
            long id=evento();inventarios.inicializarInventario(id,sector(10));
            if(cancelado)inventarios.cancelarInventarioEvento(id);else inventarios.suspender(id);
            assertThrows(IllegalStateException.class,()->inventarios.publicar(id));
            assertThrows(IllegalStateException.class,()->reservas.crearReservaTemporal(reserva(id,10)));
        }
    }
    @Test void activacionesConcurrentesConCancelacionNuncaReabrenElEvento() throws Exception {
        long id=evento();inventarios.inicializarInventario(id,sector(10));
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(3);
        try {
            var a=pool.submit(()->{start.await();try{inventarios.publicar(id);}catch(IllegalStateException expected){}return null;});
            var b=pool.submit(()->{start.await();inventarios.cancelarInventarioEvento(id);return null;});
            var c=pool.submit(()->{start.await();try{inventarios.publicar(id);}catch(IllegalStateException expected){}return null;});
            start.countDown();a.get(20,TimeUnit.SECONDS);b.get(20,TimeUnit.SECONDS);c.get(20,TimeUnit.SECONDS);
            assertThrows(IllegalStateException.class,()->inventarios.publicar(id));
            assertThrows(IllegalStateException.class,()->reservas.crearReservaTemporal(reserva(id,10)));
            assertEquals(0,inventarios.consultarDisponibilidad(id,10L).getDisponibles());
        }finally{pool.shutdownNow();}
    }
}
