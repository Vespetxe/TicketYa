package grupo22.eventocatalogo_service;

import grupo22.eventocatalogo_service.domain.*;
import grupo22.eventocatalogo_service.repository.*;
import grupo22.eventocatalogo_service.service.EventSearchService;
import grupo22.eventocatalogo_service.DTO.request.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @Transactional
class BusquedaPersistidaTest {
    @Autowired VenueRepository venues;
    @Autowired EventRepository events;
    @Autowired ParticipantRepository participants;
    @Autowired EventSearchService search;
    @Test void relevanciaPriorizaVentaHabilitadaSobreAgotadoAunqueSeaPosterior(){
        Venue v=venues.saveAndFlush(new Venue("Sala","Calle",BigDecimal.ONE,BigDecimal.ONE));
        Event exhausted=new Event("Agotado","Detalle","Recital",77L,v.getVenueId(),OffsetDateTime.now().plusDays(3));exhausted.setStatus(EventStatus.AGOTADO);events.saveAndFlush(exhausted);
        Event available=new Event("Publicado","Detalle","Recital",77L,v.getVenueId(),OffsetDateTime.now().plusDays(7));available.setStatus(EventStatus.PUBLICADO);events.saveAndFlush(available);
        var filter=new EventSearchFilterDTO();filter.setOrganizerId(77L);
        assertEquals(exhausted.getEventId(),search.searchEvents(filter,EventSortOption.DATE_ASC).get(0).getEventId());
        assertEquals(available.getEventId(),search.searchEvents(filter,EventSortOption.RELEVANCE).get(0).getEventId());
    }
    @Test void aplicaTodosLosFiltrosYOrdenamientosSobreDatosPersistidos() {
        Venue v=new Venue("Sala","Calle 1",BigDecimal.ONE,BigDecimal.ONE);v.setCity("Tandil");venues.saveAndFlush(v);
        Participant p=participants.saveAndFlush(new Participant("Artista","Banda"));
        OffsetDateTime now=OffsetDateTime.now();
        Event a=new Event("Evento A","Detalle","Recital",44L,v.getVenueId(),now.plusDays(8));a.setStatus(EventStatus.PUBLICADO);a.addParticipant(p);
        a.addSectorPrice(new EventSectorPrice(a,1L,new BigDecimal("200")));events.saveAndFlush(a);
        Event b=new Event("Evento B","Detalle","Recital",44L,v.getVenueId(),now.plusDays(9));b.setStatus(EventStatus.PUBLICADO);b.addParticipant(p);
        b.addSectorPrice(new EventSectorPrice(b,1L,new BigDecimal("100")));events.saveAndFlush(b);
        var f=new EventSearchFilterDTO("recital",44L,p.getParticipantId(),"tandil",now.plusDays(7),now.plusDays(10));
        assertEquals(2,search.searchEvents(f,EventSortOption.DATE_ASC).size());
        assertEquals(a.getEventId(),search.searchEvents(f,EventSortOption.DATE_ASC).get(0).getEventId());
        assertEquals(b.getEventId(),search.searchEvents(f,EventSortOption.DATE_DESC).get(0).getEventId());
        assertEquals(b.getEventId(),search.searchEvents(f,EventSortOption.PRICE_ASC).get(0).getEventId());
        assertEquals(a.getEventId(),search.searchEvents(f,EventSortOption.PRICE_DESC).get(0).getEventId());
        assertEquals(a.getEventId(),search.searchEvents(f,EventSortOption.RELEVANCE).get(0).getEventId());
        f.setCity("Azul");assertTrue(search.searchEvents(f,EventSortOption.DATE_ASC).isEmpty());
        f.setCity("Tandil");f.setParticipantId(Long.MAX_VALUE);assertTrue(search.searchEvents(f,EventSortOption.DATE_ASC).isEmpty());
    }
}
