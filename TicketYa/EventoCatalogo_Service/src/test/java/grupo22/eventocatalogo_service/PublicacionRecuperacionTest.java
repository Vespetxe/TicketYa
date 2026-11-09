package grupo22.eventocatalogo_service;

import com.sun.net.httpserver.HttpServer;
import grupo22.eventocatalogo_service.domain.*;
import grupo22.eventocatalogo_service.repository.*;
import grupo22.eventocatalogo_service.service.*;
import grupo22.eventocatalogo_service.DTO.response.EventResponseDTO;
import grupo22.eventocatalogo_service.exception.BusinessRuleException;
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

class PublicacionRecuperacionTest {
    record Fixture(EventWorkflow workflow,EventService service,EventResponseDTO event) {}
    private Fixture workflow(String url) {
        var service=mock(EventService.class);var repository=mock(EventRepository.class);var sectors=mock(SectorRepository.class);
        var response=new EventResponseDTO();response.setStatus(EventStatus.BORRADOR);
        when(service.getEventById(1L)).thenAnswer(i->response);
        when(service.publishEvent(1L)).thenAnswer(i->{response.setStatus(EventStatus.PUBLICADO);return response;});
        var e=new Event("Evento","Detalle","Recital",1L,2L,OffsetDateTime.now().plusDays(5));
        e.addSectorPrice(new EventSectorPrice(e,10L,BigDecimal.TEN));
        when(repository.findById(1L)).thenReturn(Optional.of(e));
        var sector=new Sector();sector.setSectorId(10L);sector.setType(SectorType.NO_NUMERADO);sector.setCapacity(2);
        when(sectors.findByVenueVenueId(2L)).thenReturn(List.of(sector));
        return new Fixture(new EventWorkflow(service,repository,sectors,mock(PlatformTransactionManager.class),url,url,url),service,response);
    }
    @Test void fallaDeHabilitacionSeRetomaSobrePublicadoSinReinicializar() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var prepared=new AtomicInteger();var enabled=new AtomicInteger();
        server.createContext("/",x->{x.getRequestBody().readAllBytes();boolean init=x.getRequestURI().getPath().endsWith("/inventario");
            int code;if(init){prepared.incrementAndGet();code=201;}else code=enabled.incrementAndGet()==1?503:200;
            x.sendResponseHeaders(code,-1);x.close();});server.start();
        try {
            var f=workflow("http://127.0.0.1:"+server.getAddress().getPort());
            assertEquals(503,assertThrows(ResponseStatusException.class,()->f.workflow.publish(1L)).getStatusCode().value());
            assertEquals(EventStatus.PUBLICADO,f.event.getStatus());
            f.workflow.publish(1L);f.workflow.publish(1L);
            verify(f.service,times(1)).publishEvent(1L);assertEquals(1,prepared.get());assertEquals(3,enabled.get());
        }finally{server.stop(0);}
    }
    @Test void inicializacionIncompatibleEsRechazoPermanenteYConservaBorrador() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",x->{x.getRequestBody().readAllBytes();x.sendResponseHeaders(400,-1);x.close();});server.start();
        try {
            var f=workflow("http://127.0.0.1:"+server.getAddress().getPort());
            assertThrows(BusinessRuleException.class,()->f.workflow.publish(1L));
            verify(f.service,never()).publishEvent(1L);assertEquals(EventStatus.BORRADOR,f.event.getStatus());
        }finally{server.stop(0);}
    }
}
