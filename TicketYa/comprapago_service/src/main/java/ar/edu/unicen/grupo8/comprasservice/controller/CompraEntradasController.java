package ar.edu.unicen.grupo8.comprasservice.controller;

import ar.edu.unicen.grupo8.comprasservice.client.EntradaClient;
import ar.edu.unicen.grupo8.comprasservice.service.CompraService;
import ar.edu.unicen.grupo8.comprasservice.service.EntradaIdentidadService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
public class CompraEntradasController {
    private final CompraService compras;
    private final EntradaClient entradas;
    private final EntradaIdentidadService identidad;
    public CompraEntradasController(CompraService c,EntradaClient e,EntradaIdentidadService i){compras=c;entradas=e;identidad=i;}
    public record EntradaSeleccionable(Long entradaId,String ticketId,Long sectorId,Long asientoId,String estado) {}
    @GetMapping("/compras/{id}/entradas")
    @Operation(summary="Consultar entradas seleccionables para devolucion",description="Asocia por unidad el ID numerico aceptado en entradasIds, UUID externo y asiento operativo. No inferir correspondencias por posicion de listas.")
    public List<EntradaSeleccionable> consultar(@PathVariable Long id) {
        compras.obtenerCompraPorId(id);
        java.util.List<ar.edu.unicen.grupo8.comprasservice.dto.EntradaDTO> values;
        try{values=entradas.consultarEntradas(id);}
        catch(org.springframework.web.client.RestClientException ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo consultar Entradas",ex);}
        if(values==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo consultar Entradas");
        return values.stream().map(e->{
            if(!id.equals(e.compraId()))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Respuesta de Entradas incompatible");
            return new EntradaSeleccionable(e.entradaId(),identidad.uuid(e.entradaId()),e.sectorId(),e.asientoId(),e.estado());
        }).toList();
    }
}
