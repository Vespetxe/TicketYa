package grupo22.eventocatalogo_service;
import com.sun.net.httpserver.HttpServer;
import grupo22.eventocatalogo_service.domain.*;
import grupo22.eventocatalogo_service.repository.*;
import grupo22.eventocatalogo_service.service.*;
import grupo22.eventocatalogo_service.DTO.response.EventResponseDTO;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PublicacionInterrumpidaTest {
    @Test void noPublicaConUnSectorPendienteYReintentaElInventarioCompleto()throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var calls=new AtomicInteger();
        server.createContext("/",exchange->{exchange.getRequestBody().readAllBytes();int n=calls.incrementAndGet();exchange.sendResponseHeaders(n==2?503:201,-1);exchange.close();});server.start();
        try {
            var service=mock(EventService.class);var repository=mock(EventRepository.class);var sectors=mock(SectorRepository.class);
            var response=new EventResponseDTO();response.setStatus(EventStatus.BORRADOR);when(service.getEventById(1L)).thenReturn(response);when(service.publishEvent(1L)).thenReturn(response);
            var e=new Event("Evento","Detalle","Recital",1L,2L,OffsetDateTime.now().plusDays(5));
            e.addSectorPrice(new EventSectorPrice(e,10L,BigDecimal.TEN));e.addSectorPrice(new EventSectorPrice(e,11L,BigDecimal.TEN));
            when(repository.findById(1L)).thenReturn(Optional.of(e));
            var a=new Sector();a.setSectorId(10L);a.setType(SectorType.NO_NUMERADO);a.setCapacity(2);
            var b=new Sector();b.setSectorId(11L);b.setType(SectorType.NO_NUMERADO);b.setCapacity(2);
            when(sectors.findByVenueVenueId(2L)).thenReturn(List.of(a,b));
            String url="http://127.0.0.1:"+server.getAddress().getPort();
            var workflow=new EventWorkflow(service,repository,sectors,mock(PlatformTransactionManager.class),url,url,url);
            assertThrows(ResponseStatusException.class,()->workflow.publish(1L));verify(service,never()).publishEvent(1L);
            workflow.publish(1L);verify(service,times(1)).publishEvent(1L);assertEquals(5,calls.get());
        }finally{server.stop(0);}
    }
}
