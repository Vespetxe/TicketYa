package ar.edu.unicen.grupo8.comprasservice.controller;
import ar.edu.unicen.grupo8.comprasservice.service.CompraService;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
public class OrdenInternaController {
    private final CompraService compras;
    public OrdenInternaController(CompraService compras){this.compras=compras;}
    public record Item(String eventId,String sectorId,String seatId,int quantity){}
    public record Order(String orderId,String clientId,String status,List<Item> items){}
    @GetMapping("/internal/orders/{id}")
    public Order get(@PathVariable Long id){
        var c=compras.obtenerCompraPorId(id);
        boolean habilitada=!c.isCancelacionSolicitada() && ((c.getEstado()==EstadoCompra.PROCESANDO && c.getEtapa()==EtapaCompra.EMISION) || c.getEstado()==EstadoCompra.COMPLETADA);
        return new Order(id.toString(),c.getClienteId().toString(),habilitada?"CONFIRMADA":c.getEstado().name(),c.getDetalles().stream()
                .map(d->new Item(c.getEventoId().toString(),d.getSectorId().toString(),d.getAsientoId()==null?null:d.getAsientoId().toString(),d.getCantidad())).toList());
    }
}
