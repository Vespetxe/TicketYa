
package ar.edu.unicen.grupo8.comprasservice.client;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Primary
public class EventoClientSimulado implements EventoClient {

    public record Programacion(java.time.LocalDateTime fechaInicio, boolean cancelado) {
        public Programacion(java.time.LocalDateTime fechaInicio) { this(fechaInicio, false); }
    }
    private final AlmacenSimulado almacen;
    private final java.time.Clock clock;

    public EventoClientSimulado() { this(new AlmacenSimulado.Memoria(), java.time.Clock.systemUTC()); }

    @org.springframework.beans.factory.annotation.Autowired
    public EventoClientSimulado(AlmacenSimulado almacen, java.time.Clock clock) {
        this.almacen = almacen; this.clock = clock;
    }

    // Catálogo de esta entrega; las cancelaciones se registran únicamente en el simulador.
    public enum EstadoComercial { PUBLICADO, NO_PUBLICADO, CANCELADO, FINALIZADO }
    private static final java.util.Map<Long, EstadoComercial> EVENTOS = java.util.Map.of(
            20L, EstadoComercial.PUBLICADO, 21L, EstadoComercial.PUBLICADO,
            40L, EstadoComercial.PUBLICADO, 200L, EstadoComercial.PUBLICADO,
            901L, EstadoComercial.NO_PUBLICADO, 902L, EstadoComercial.CANCELADO,
            903L, EstadoComercial.FINALIZADO);
    private static final java.util.Map<Long, BigDecimal> PRECIOS = java.util.Map.of(
            1L, new BigDecimal("15000.00"), 2L, new BigDecimal("25000.00"),
            3L, new BigDecimal("40000.00"));

    @Override
    public boolean eventoValido(Long eventoId) {
        boolean valido = eventoId != null && EVENTOS.get(eventoId) == EstadoComercial.PUBLICADO;
        // Fijar la fecha al primer uso comercial evita extender el plazo al reiniciar el cliente.
        if (valido) { obtenerFechaInicio(eventoId); valido = !eventoCancelado(eventoId); }
        return valido;
    }

    @Override
    public java.time.LocalDateTime obtenerFechaInicio(Long eventoId) {
        if (eventoId == null || !EVENTOS.containsKey(eventoId)) throw new IllegalArgumentException("El evento indicado no existe");
        return almacen.actualizar("evento:" + eventoId, Programacion.class, anterior -> anterior != null ? anterior :
                new Programacion(java.time.LocalDateTime.now(clock).plusDays(
                        EVENTOS.get(eventoId) == EstadoComercial.FINALIZADO ? -1 : 7))).fechaInicio();
    }

    @Override
    public BigDecimal obtenerPrecioSector(
            Long eventoId,
            Long sectorId
    ) {

        if (!eventoValido(eventoId)
                || sectorId == null
                || sectorId <= 0) {
            throw new IllegalArgumentException(
                    "Evento o sector invalido"
            );
        }

        BigDecimal precio = PRECIOS.get(sectorId);
        if (precio == null) throw new IllegalArgumentException("El sector indicado no existe");
        return precio;
    }

    @Override
    public boolean eventoCancelado(Long id) {
        obtenerFechaInicio(id);
        return EVENTOS.get(id) == EstadoComercial.CANCELADO
                || almacen.actualizar("evento:" + id, Programacion.class, v -> v).cancelado();
    }
    @Override
    public void registrarCancelacion(Long id) {
        obtenerFechaInicio(id);
        almacen.actualizar("evento:" + id, Programacion.class, v -> new Programacion(v.fechaInicio(), true));
    }

}
