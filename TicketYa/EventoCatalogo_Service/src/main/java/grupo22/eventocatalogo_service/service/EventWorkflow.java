package grupo22.eventocatalogo_service.service;

import grupo22.eventocatalogo_service.DTO.response.EventResponseDTO;
import grupo22.eventocatalogo_service.domain.*;
import grupo22.eventocatalogo_service.repository.*;
import grupo22.eventocatalogo_service.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Service
public class EventWorkflow {
    private final EventService events;
    private final EventRepository repository;
    private final SectorRepository sectors;
    private final TransactionTemplate tx;
    private final RestClient inventory,compras,entradas;
    record SeatSnapshot(Long catalogoAsientoId,String fila,Integer numero) {}
    record StockSnapshot(Long idSector,boolean esNumerado,int capacidadTotal,List<SeatSnapshot> asientos) {}
    public EventWorkflow(EventService events,EventRepository repository,SectorRepository sectors,PlatformTransactionManager manager,
            @Value("${ticketya.inventario.base-url:http://localhost:8082}") String inventoryUrl,
            @Value("${ticketya.compras.base-url:http://localhost:8084}") String comprasUrl,
            @Value("${ticketya.entradas.base-url:http://localhost:8083}") String entradasUrl) {
        this.events=events;this.repository=repository;this.sectors=sectors;tx=new TransactionTemplate(manager);
        var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(2000);factory.setReadTimeout(10000);
        inventory=RestClient.builder().requestFactory(factory).baseUrl(inventoryUrl).build();
        compras=RestClient.builder().requestFactory(factory).baseUrl(comprasUrl).build();
        entradas=RestClient.builder().requestFactory(factory).baseUrl(entradasUrl).build();
    }
    public EventResponseDTO publish(Long id) {
        var event=events.getEventById(id);
        if(event.getStatus()==EventStatus.PUBLICADO){habilitarPublicacion(id);return events.getEventById(id);}
        if(event.getStatus()!=EventStatus.BORRADOR)throw new BusinessRuleException("Solo se publica un borrador");
        List<StockSnapshot> stock=tx.execute(status->{
            var e=repository.findById(id).orElseThrow();
            var list=sectors.findByVenueVenueId(e.getVenueId());
            if(list.isEmpty())throw new BusinessRuleException("El lugar requiere sectores");
            var prices=e.getSectorPrices().stream().map(EventSectorPrice::getSectorId).toList();
            if(list.stream().anyMatch(s->!prices.contains(s.getSectorId())))throw new BusinessRuleException("Faltan precios");
            return list.stream().map(s->new StockSnapshot(s.getSectorId(),s.getType()==SectorType.NUMERADO,s.getCapacity(),
                    s.getSeats().stream().map(a->new SeatSnapshot(a.getSeatId(),a.getRowLabel(),a.getSeatNumber())).toList())).toList();
        });
        try {
            for(var sector:stock)inventory.post().uri("/eventos/{id}/inventario",id).body(sector).retrieve().toBodilessEntity();
        } catch(org.springframework.web.client.RestClientResponseException ex) {
            if(ex.getStatusCode().is4xxClientError())throw new BusinessRuleException("Inicializacion de inventario incompatible o bloqueada; no reintentar sin revisar la configuracion");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo inicializar el inventario; reintente",ex);
        } catch(org.springframework.web.client.RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo inicializar el inventario; reintente",ex);
        }
        try {events.publishEvent(id);}
        catch(org.springframework.dao.OptimisticLockingFailureException | BusinessRuleException race){
            if(events.getEventById(id).getStatus()!=EventStatus.PUBLICADO)throw race;
        }
        habilitarPublicacion(id);
        return events.getEventById(id);
    }
    private void habilitarPublicacion(Long id) {
        try {inventory.post().uri("/internal/eventos/{id}/publicar",id).retrieve().toBodilessEntity();}
        catch(org.springframework.web.client.RestClientResponseException ex){
            if(ex.getStatusCode().is4xxClientError())throw new BusinessRuleException("Publicacion bloqueada por un estado de inventario incompatible");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Evento publicado con habilitacion pendiente; reintente publicar",ex);
        }catch(org.springframework.web.client.RestClientException ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Evento publicado con habilitacion pendiente; reintente publicar",ex);}
    }
    public EventResponseDTO cancel(Long id,String role,Long administradorId) {
        if(administradorId==null || administradorId<=0)throw new BusinessRuleException("Se requiere X-Admin-Id positivo");
        var event=events.getEventById(id);
        if(!"ADMIN".equalsIgnoreCase(role))throw new BusinessRuleException("Se requiere identificar administrador");
        if(event.getStatus()==EventStatus.FINALIZADO)throw new BusinessRuleException("Evento finalizado");
        try {entradas.post().uri("/internal/events/{id}/cancel-emission",id).retrieve().toBodilessEntity();}
        catch(org.springframework.web.client.RestClientException ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo bloquear la emision; reintente",ex);}
        if(event.getStatus()!=EventStatus.CANCELADO) {
            try {event=events.cancelEvent(id,role);}
            catch(org.springframework.dao.OptimisticLockingFailureException | BusinessRuleException race){
                event=events.getEventById(id);if(event.getStatus()!=EventStatus.CANCELADO)throw race;
            }
        }
        try {
            inventory.post().uri("/eventos/{id}/cancelar-inventario",id).retrieve().toBodilessEntity();
            var result=compras.post().uri("/admin/eventos/{id}/cancelacion",id).header("X-Admin-Id",administradorId.toString()).retrieve().toBodilessEntity();
            if(result.getStatusCode().value()==202)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cancelacion pendiente; reintente");
        } catch(org.springframework.web.client.RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cancelacion registrada con coordinacion pendiente; reintente",ex);
        }
        return event;
    }
    public EventResponseDTO suspend(Long id,String role) {
        var event=events.suspendOrUpdateStatus(id,"SUSPENDIDO",role);
        try {inventory.post().uri("/internal/eventos/{id}/suspender",id).retrieve().toBodilessEntity();}
        catch(org.springframework.web.client.RestClientException ex) {throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Suspension pendiente; reintente",ex);}
        return event;
    }
    public EventResponseDTO status(Long id,String status,String role){
        if(!"ADMIN".equalsIgnoreCase(role))throw new BusinessRuleException("Se requiere identificar administrador");
        if("PUBLICADO".equals(status)){
            if(events.getEventById(id).getStatus()!=EventStatus.AGOTADO)throw new BusinessRuleException("Solo se reabre por esta operacion un evento agotado");
            try{inventory.post().uri("/internal/eventos/{id}/habilitar",id).retrieve().toBodilessEntity();}
            catch(org.springframework.web.client.RestClientException ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo reabrir el inventario",ex);}
            return events.suspendOrUpdateStatus(id,status,role);
        }
        if(!Set.of("AGOTADO","FINALIZADO").contains(status))throw new BusinessRuleException("Operacion no permitida; usar publicar, suspender o cancelar");
        var event=events.suspendOrUpdateStatus(id,status,role);
        try{inventory.post().uri("/internal/eventos/{id}/suspender",id).retrieve().toBodilessEntity();}
        catch(org.springframework.web.client.RestClientException ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Estado registrado con bloqueo pendiente; reintente",ex);}
        return event;
    }
}
