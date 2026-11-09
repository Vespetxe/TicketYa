package ar.edu.unicen.grupo8.comprasservice.client;
import ar.edu.unicen.grupo8.comprasservice.dto.*;
import ar.edu.unicen.grupo8.comprasservice.service.EntradaIdentidadService;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import java.time.*;
import java.util.*;
@Component @Profile("integracion")
public class RestReservaClient implements ReservaClient {
    record Asiento(Long idAsiento,String fila,Integer numero){}
    record Reserva(Long idReserva,Long idCliente,Long idEvento,Long idSector,int cantidad,LocalDateTime fechaExpiracion,String estado,List<Asiento> asientosReservados){}
    private final RestClient rest;private final EntradaIdentidadService identidad;
    public RestReservaClient(@Value("${ticketya.inventario.base-url:http://localhost:8082}") String url,EntradaIdentidadService identidad){rest=RestSupport.client(url);this.identidad=identidad;}
    public ReservaDTO consultarReserva(Long id){
        var r=rest.get().uri("/reservas/{id}",id).retrieve().body(Reserva.class);
        if(r==null)throw new IllegalStateException("Reserva desconocida");
        var detalles=r.asientosReservados()!=null && !r.asientosReservados().isEmpty()
                ?r.asientosReservados().stream().map(a->new DetalleEmisionDTO(r.idSector(),a.idAsiento(),1)).toList()
                :List.of(new DetalleEmisionDTO(r.idSector(),null,r.cantidad()));
        return new ReservaDTO(r.idReserva(),r.idCliente(),r.idEvento(),"PENDIENTE".equals(r.estado())?"TEMPORAL":r.estado(),r.fechaExpiracion(),detalles);
    }
    public boolean reservaValida(Long id,Long cliente,Long evento){var r=consultarReserva(id);return cliente.equals(r.clienteId()) && evento.equals(r.eventoId()) && "TEMPORAL".equals(r.estado()) && r.fechaVencimiento().isAfter(LocalDateTime.now(Clock.systemUTC()));}
    public void confirmarReserva(Long id){RestSupport.mutate(()->rest.post().uri("/reservas/{id}/confirmar",id).retrieve().toBodilessEntity());}
    public void liberarReserva(Long id){RestSupport.mutate(()->rest.post().uri("/reservas/{id}/liberar",id).retrieve().toBodilessEntity());}
    public void compensarReserva(Long id){RestSupport.mutate(()->rest.post().uri("/internal/reservas/{id}/compensar",id).retrieve().toBodilessEntity());}
    public void liberarLugares(Long id,List<EntradaDTO> entradas){
        record Unidad(String entradaId,Long asientoId){}
        RestSupport.mutate(()->rest.post().uri("/internal/reservas/{id}/devoluciones",id)
                .body(Map.of("entradas",entradas.stream().map(e->new Unidad(identidad.uuid(e.entradaId()),e.asientoId())).toList())).retrieve().toBodilessEntity());
    }
    public List<Long> consultarLugaresLiberados(Long id){
        String[] ids=rest.get().uri("/internal/reservas/{id}/devoluciones",id).retrieve().body(String[].class);
        if(ids==null)throw new IllegalStateException("Liberaciones desconocidas");
        return Arrays.stream(ids).map(identidad::registrar).toList();
    }
}
