package ar.edu.unicen.grupo8.comprasservice.client;

import ar.edu.unicen.grupo8.comprasservice.dto.*;
import ar.edu.unicen.grupo8.comprasservice.exception.OperacionExternaRechazadaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.List;

@Component
@Primary
@org.springframework.context.annotation.Profile("!integracion")
public class ReservaClientSimulado implements ReservaClient {
    private final AlmacenSimulado almacen;
    private final Clock clock;
    public ReservaClientSimulado() { this(new AlmacenSimulado.Memoria(), Clock.systemUTC()); }
    @Autowired
    public ReservaClientSimulado(AlmacenSimulado almacen, Clock clock) { this.almacen = almacen; this.clock = clock; }

    @Override
    public boolean reservaValida(Long id, Long cliente, Long evento) {
        if (id == null || id <= 0 || cliente == null || evento == null) return false;
        try {
            ReservaDTO r = consultarReserva(id);
            return "TEMPORAL".equals(r.estado()) && cliente.equals(r.clienteId()) && evento.equals(r.eventoId())
                    && r.fechaVencimiento().isAfter(LocalDateTime.now(clock));
        } catch (IllegalArgumentException e) { return false; }
    }

    @Override
    public ReservaDTO consultarReserva(Long id) {
        validarId(id);
        return almacen.actualizar("reserva:" + id, ReservaDTO.class, r -> r == null ? inicial(id) : r);
    }

    private ReservaDTO inicial(Long id) {
        long cliente = 10, evento = 20;
        int cantidad = 2;
        if (id == 3) { cliente = 30; evento = 40; cantidad = 1; }
        else if (id == 31) { evento = 21; cantidad = 1; }
        else if (id == 32) { cliente = 11; cantidad = 1; }
        else if (id == 301 || id == 302) { cliente = 100; evento = 200; cantidad = 4; }
        else if (id != 1 && id != 2 && id != 30) throw new IllegalArgumentException("No existe una reserva con ID: " + id);
        return new ReservaDTO(id, cliente, evento, "TEMPORAL",
                LocalDateTime.now(clock).plusMinutes(id == 2 ? -15 : 15),
                List.of(new DetalleEmisionDTO(1L, null, cantidad)));
    }

    @Override
    public void confirmarReserva(Long id) {
        consultarReserva(id);
        almacen.actualizar("reserva:" + id, ReservaDTO.class, r -> {
            if ("CONFIRMADA".equals(r.estado())) return r;
            if (!"TEMPORAL".equals(r.estado())) throw new OperacionExternaRechazadaException("La reserva no se encuentra en estado temporal");
            if (!r.fechaVencimiento().isAfter(LocalDateTime.now(clock))) throw new OperacionExternaRechazadaException("La reserva se encuentra vencida");
            return conEstado(r, "CONFIRMADA");
        });
    }

    @Override
    public void liberarReserva(Long id) {
        consultarReserva(id);
        almacen.actualizar("reserva:" + id, ReservaDTO.class, r -> {
            if ("LIBERADA".equals(r.estado())) return r;
            if (!"TEMPORAL".equals(r.estado())) throw new OperacionExternaRechazadaException("No se puede liberar una reserva confirmada");
            return conEstado(r, "LIBERADA");
        });
    }

    /** Solo compensación de compra fallida: no implementa devoluciones. */
    @Override
    public void compensarReserva(Long id) {
        consultarReserva(id);
        almacen.actualizar("reserva:" + id, ReservaDTO.class, r -> conEstado(r, "LIBERADA"));
    }

    private ReservaDTO conEstado(ReservaDTO r, String estado) {
        return new ReservaDTO(r.reservaId(), r.clienteId(), r.eventoId(), estado, r.fechaVencimiento(), r.detalles());
    }

    public void registrarReserva(ReservaDTO reserva) {
        validarId(reserva.reservaId());
        almacen.actualizar("reserva:" + reserva.reservaId(), ReservaDTO.class, anterior -> {
            if (anterior != null) throw new IllegalStateException("La reserva ya existe");
            return reserva;
        });
    }
    public void reiniciarReservas() { almacen.limpiar("reserva:"); almacen.limpiar("lugares:"); }
    private void validarId(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("El identificador de reserva debe ser positivo");
    }

    public record Lugares(java.util.Map<Long, EntradaDTO> entradas) { }
    @Override
    public void liberarLugares(Long id, List<EntradaDTO> entradas) {
        var reserva = consultarReserva(id);
        if (!"CONFIRMADA".equals(reserva.estado())) throw new IllegalStateException("La reserva no esta confirmada");
        if (entradas == null || entradas.isEmpty()) throw new IllegalArgumentException("Seleccion vacia");
        almacen.actualizar("lugares:" + id, Lugares.class, anterior -> {
            var todos = new java.util.HashMap<Long, EntradaDTO>(anterior == null ? java.util.Map.of() : anterior.entradas());
            for (var e : entradas) {
                if (e == null || e.entradaId() == null || e.entradaId() <= 0 || !"ANULADA".equals(e.estado())
                        || !reserva.clienteId().equals(e.clienteId()) || !reserva.eventoId().equals(e.eventoId()))
                    throw new IllegalArgumentException("Entrada incompatible con la reserva");
                var previo = todos.putIfAbsent(e.entradaId(), e);
                if (previo != null && !previo.equals(e)) throw new IllegalStateException("Entrada reutilizada con otros datos");
            }
            var liberados = todos.values().stream().map(e -> new DetalleEmisionDTO(e.sectorId(), e.asientoId(), 1)).toList();
            var limite = ar.edu.unicen.grupo8.comprasservice.service.SeleccionReserva.agrupar(reserva.detalles());
            var seleccion = ar.edu.unicen.grupo8.comprasservice.service.SeleccionReserva.agrupar(liberados);
            seleccion.forEach((sector, cantidad) -> {
                if (cantidad > limite.getOrDefault(sector, 0L)) throw new IllegalStateException("Los lugares superan la reserva");
            });
            return new Lugares(java.util.Map.copyOf(todos));
        });
    }
    @Override
    public List<Long> consultarLugaresLiberados(Long id) {
        validarId(id);
        var r = almacen.actualizar("lugares:" + id, Lugares.class, v -> v);
        return r == null ? List.of() : r.entradas().keySet().stream().sorted().toList();
    }

}
