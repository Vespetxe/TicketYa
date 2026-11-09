package ar.edu.unicen.grupo8.comprasservice.client;
import ar.edu.unicen.grupo8.comprasservice.dto.*;
import ar.edu.unicen.grupo8.comprasservice.service.EntradaIdentidadService;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import java.util.*;
@Component @Profile("integracion")
public class RestEntradaClient implements EntradaClient {
    private final RestClient rest;private final EntradaIdentidadService identidad;
    public record Ticket(String ticketId,String orderId,String eventId,String clientId,String sectorId,String seatId,String status,String qr){}
    public RestEntradaClient(@Value("${ticketya.entradas.base-url:http://localhost:8083}") String url,EntradaIdentidadService identidad){rest=RestSupport.client(url);this.identidad=identidad;}
    public List<EntradaDTO> consultarEntradas(Long id){
        Ticket[] values=rest.get().uri("/tickets?orderId={id}",id).retrieve().body(Ticket[].class);
        if(values==null)throw new IllegalStateException("Respuesta de Entradas desconocida");
        return Arrays.stream(values).map(t->new EntradaDTO(registrar(t.ticketId()),Long.valueOf(t.orderId()),Long.valueOf(t.eventId()),Long.valueOf(t.clientId()),
                Long.valueOf(t.sectorId()),t.seatId()==null?null:Long.valueOf(t.seatId()),t.status(),t.qr())).toList();
    }
    private Long registrar(String uuid){try{return identidad.registrar(uuid);}catch(org.springframework.dao.DataIntegrityViolationException race){return identidad.registrar(uuid);}}
    public List<EntradaDTO> solicitarEntradas(Long compra,Long evento,Long cliente,List<DetalleEmisionDTO> detalles){
        RestSupport.mutate(()->rest.post().uri("/internal/tickets/issue").body(Map.of("orderId",compra.toString())).retrieve().toBodilessEntity());
        return consultarEntradas(compra);
    }
    public void anularEntradas(Long compra){RestSupport.mutate(()->rest.post().uri("/internal/tickets/orders/{id}/annul",compra).retrieve().toBodilessEntity());}
    public void anularEntradas(Long compra,List<Long> ids){for(Long id:ids)RestSupport.mutate(()->rest.patch().uri("/internal/tickets/{id}/status",identidad.uuid(id))
            .body(Map.of("orderId",compra.toString(),"status","ANULADA")).retrieve().toBodilessEntity());}
}
