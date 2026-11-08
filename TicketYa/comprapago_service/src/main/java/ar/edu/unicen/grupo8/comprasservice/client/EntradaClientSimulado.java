package ar.edu.unicen.grupo8.comprasservice.client;

import ar.edu.unicen.grupo8.comprasservice.dto.*;
import ar.edu.unicen.grupo8.comprasservice.service.SeleccionReserva;
import ar.edu.unicen.grupo8.comprasservice.exception.OperacionExternaRechazadaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@Primary
public class EntradaClientSimulado implements EntradaClient {
    public record Emision(Long evento, Long cliente, List<DetalleEmisionDTO> detalles, List<EntradaDTO> entradas, boolean anulada) { }
    private final AlmacenSimulado almacen;
    public EntradaClientSimulado() { this(new AlmacenSimulado.Memoria()); }
    @Autowired
    public EntradaClientSimulado(AlmacenSimulado almacen) { this.almacen = almacen; }

    @Override
    public List<EntradaDTO> solicitarEntradas(Long compra, Long evento, Long cliente, List<DetalleEmisionDTO> detalles) {
        validarId(compra); validarId(evento); validarId(cliente);
        var seleccion = SeleccionReserva.agrupar(detalles);
        return almacen.actualizar("entrada:" + compra, Emision.class, anterior -> {
            if (anterior != null) {
                if (anterior.anulada()) throw new OperacionExternaRechazadaException("No se pueden volver a emitir entradas anuladas");
                if (!evento.equals(anterior.evento()) || !cliente.equals(anterior.cliente())
                        || !seleccion.equals(SeleccionReserva.agrupar(anterior.detalles())))
                    throw new IllegalStateException("La compra ya tiene otra solicitud de emision");
                if (anterior.entradas().stream().anyMatch(e -> "ANULADA".equals(e.estado())))
                    throw new OperacionExternaRechazadaException("No se pueden volver a emitir entradas anuladas");
                return anterior;
            }
            List<EntradaDTO> entradas = new ArrayList<>();
            for (var d : detalles) for (int i = 0; i < d.cantidad(); i++) {
                long indice = entradas.size() + 1L;
                long id = Math.addExact(Math.multiplyExact(compra, 100000L), indice);
                entradas.add(new EntradaDTO(id, compra, evento, cliente, d.sectorId(), d.asientoId(), "EMITIDA", "ENT-" + id));
            }
            return new Emision(evento, cliente, List.copyOf(detalles), List.copyOf(entradas), false);
        }).entradas();
    }

    @Override
    public List<EntradaDTO> consultarEntradas(Long compra) {
        validarId(compra);
        Emision emision = almacen.actualizar("entrada:" + compra, Emision.class, e -> e);
        return emision == null ? List.of() : emision.entradas();
    }

    @Override
    public void anularEntradas(Long compra) {
        validarId(compra);
        // La anulación es también una barrera durable contra solicitudes de emisión demoradas.
        almacen.actualizar("entrada:" + compra, Emision.class, e -> e == null ? new Emision(null, null, List.of(), List.of(), true) :
                new Emision(e.evento(), e.cliente(), e.detalles(), e.entradas().stream().map(t ->
                        new EntradaDTO(t.entradaId(), t.compraId(), t.eventoId(), t.clienteId(), t.sectorId(),
                                t.asientoId(), "ANULADA", t.codigo())).toList(), true));
    }
    public void reiniciarEntradas() { almacen.limpiar("entrada:"); }
    private void validarId(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("El identificador debe ser positivo");
    }

    @Override
    public void anularEntradas(Long compra, List<Long> ids) {
        validarId(compra);
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)
                || new HashSet<>(ids).size() != ids.size()) throw new IllegalArgumentException("Seleccion invalida");
        almacen.actualizar("entrada:" + compra, Emision.class, e -> {
            if (e == null || !e.entradas().stream().map(EntradaDTO::entradaId).toList().containsAll(ids))
                throw new IllegalStateException("Las entradas no pertenecen a la compra");
            return new Emision(e.evento(), e.cliente(), e.detalles(), e.entradas().stream().map(t ->
                    ids.contains(t.entradaId()) ? new EntradaDTO(t.entradaId(), t.compraId(), t.eventoId(), t.clienteId(),
                            t.sectorId(), t.asientoId(), "ANULADA", t.codigo()) : t).toList(), e.anulada());
        });
    }

}
