package ar.edu.unicen.grupo8.comprasservice.client;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
@Component @Profile("integracion")
public class RestEventoClient implements EventoClient {
    record Precio(Long sectorId,BigDecimal price){}
    record Evento(Long eventId,String status,OffsetDateTime startDateTime,List<Precio> sectorPrices){}
    private final RestClient rest;
    public RestEventoClient(@Value("${ticketya.eventos.base-url:http://localhost:8081}") String url){rest=RestSupport.client(url);}
    private Evento get(Long id){var e=rest.get().uri("/api/v1/events/{id}",id).retrieve().body(Evento.class);if(e==null)throw new IllegalStateException("Evento desconocido");return e;}
    public boolean eventoValido(Long id){var e=get(id);return "PUBLICADO".equals(e.status()) && e.startDateTime().isAfter(OffsetDateTime.now(ZoneOffset.UTC));}
    public LocalDateTime obtenerFechaInicio(Long id){return get(id).startDateTime().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();}
    public BigDecimal obtenerPrecioSector(Long id,Long sector){var e=get(id);if(!"PUBLICADO".equals(e.status()))throw new IllegalArgumentException("Evento no publicado");return e.sectorPrices().stream().filter(p->sector.equals(p.sectorId())).findFirst().orElseThrow(()->new IllegalArgumentException("Sector inexistente")).price();}
    public boolean eventoCancelado(Long id){return "CANCELADO".equals(get(id).status());}
    public void registrarCancelacion(Long id){if(!eventoCancelado(id))throw new IllegalArgumentException("Eventos no confirma la cancelacion");}
}
