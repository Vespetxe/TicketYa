package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.dto.*;
import ar.edu.unicen.grupo8.comprasservice.entity.*;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.*;
import ar.edu.unicen.grupo8.comprasservice.exception.*;
import ar.edu.unicen.grupo8.comprasservice.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Persistencia y validaciones locales. No ejecuta llamadas externas ni reintegros. */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
public class DevolucionRegistroService {
    private final CompraRepository compras;
    private final DevolucionRepository devoluciones;
    private final DetalleDevolucionRepository detalles;
    private final Clock clock;

    public DevolucionRegistroService(CompraRepository compras, DevolucionRepository devoluciones,
            DetalleDevolucionRepository detalles, Clock clock) {
        this.compras = compras; this.devoluciones = devoluciones; this.detalles = detalles; this.clock = clock;
    }

    public DevolucionDTO registrar(SolicitarDevolucionDTO dto, List<EntradaDTO> emitidas,
            LocalDateTime inicioEvento, LocalDateTime fechaSolicitud) {
        return registrarInterno(dto, emitidas, inicioEvento, fechaSolicitud, false);
    }

    private DevolucionDTO registrarInterno(SolicitarDevolucionDTO dto, List<EntradaDTO> emitidas,
            LocalDateTime inicioEvento, LocalDateTime fechaSolicitud, boolean cancelacion) {
        Compra compra = compras.buscarPorIdConBloqueo(dto.compraId())
                .orElseThrow(() -> new CompraNoEncontradaException(dto.compraId()));
        validarCompra(compra, dto.clienteId());
        if (!cancelacion && inicioEvento == null) throw new IllegalStateException("No se conoce la fecha de inicio del evento");
        if (!cancelacion && inicioEvento.isBefore(fechaSolicitud.plusHours(72)))
            throw new IllegalArgumentException("La devolucion debe solicitarse al menos 72 horas antes del evento");
        if (new HashSet<>(dto.entradasIds()).size() != dto.entradasIds().size())
            throw new IllegalArgumentException("La solicitud contiene entradas repetidas");
        validarEntradas(compra, emitidas);
        List<DetalleDevolucion> nuevos = new ArrayList<>();
        for (Long entradaId : dto.entradasIds()) {
            if (!compra.getEntradasIds().contains(entradaId)) throw new IllegalArgumentException("La entrada no pertenece a la compra");
            EntradaDTO entrada = emitidas.stream().filter(e -> e.entradaId().equals(entradaId)).findFirst().orElseThrow();
            if (!"EMITIDA".equals(entrada.estado())) throw new IllegalStateException("La entrada no esta vigente para devolver");
            if (detalles.existsByEntradaIdAndDevolucionEstadoNot(entradaId, EstadoDevolucion.RECHAZADA))
                throw new IllegalStateException("La entrada ya tiene una devolucion activa o completada");
            DetalleCompra original = asociarDetalle(compra, entrada, emitidas);
            DetalleDevolucion detalle = new DetalleDevolucion();
            detalle.setDetalleCompra(original); detalle.setEntradaId(entradaId); detalle.setImporte(original.getPrecioUnitario());
            nuevos.add(detalle);
        }
        Devolucion devolucion = new Devolucion();
        devolucion.setCompra(compra); devolucion.setFechaSolicitud(fechaSolicitud); devolucion.setMotivo(dto.motivo());
        devolucion.setImporteSolicitado(nuevos.stream().map(DetalleDevolucion::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add));
        devoluciones.save(devolucion);
        nuevos.forEach(d -> d.setDevolucion(devolucion)); detalles.saveAll(nuevos);
        return convertir(devolucion, nuevos);
    }

    /** La notificación ya fue registrada en Eventos antes de entrar a esta transacción. */
    public List<Long> prepararCancelacion(Long compraId, List<EntradaDTO> emitidas) {
        Compra c = compras.buscarPorIdConBloqueo(compraId).orElseThrow(() -> new CompraNoEncontradaException(compraId));
        if (c.getEjecutor() != null && c.getBloqueoHasta() != null && c.getBloqueoHasta().isAfter(LocalDateTime.now(clock)))
            throw new IllegalStateException("La compra tiene una operacion en curso");
        if (c.getEstado() != EstadoCompra.COMPLETADA && c.getEstado() != EstadoCompra.DEVUELTA_PARCIAL
                && c.getEstado() != EstadoCompra.DEVUELTA_TOTAL) return List.of();
        validarEntradas(c, emitidas);
        List<Long> ids = new ArrayList<>();
        for (var devolucionId : devoluciones.buscarIdsPorCompra(compraId)) {
            var d = devoluciones.buscarConBloqueo(devolucionId).orElseThrow();
            if (d.getEstado() == EstadoDevolucion.RECHAZADA) continue;
            ids.add(d.getId());
            if (d.getEstado() == EstadoDevolucion.COMPLETADA) continue;
            d.setCancelacionEvento(true);
            if (d.getEstado() == EstadoDevolucion.PENDIENTE) {
                d.setEstado(EstadoDevolucion.APROBADA); d.setMotivoResolucion("Cancelacion de evento");
                d.setFechaResolucion(LocalDateTime.now(clock));
            }
        }
        var restantes = emitidas.stream().filter(e -> "EMITIDA".equals(e.estado()))
                .map(EntradaDTO::entradaId).filter(id -> !detalles.existsByEntradaIdAndDevolucionEstadoNot(id, EstadoDevolucion.RECHAZADA)).toList();
        if (!restantes.isEmpty()) {
            var dto = registrarInterno(new SolicitarDevolucionDTO(c.getId(), c.getClienteId(), restantes,
                    "Cancelacion de evento"), emitidas, null, LocalDateTime.now(clock), true);
            var d = devoluciones.findById(dto.id()).orElseThrow();
            d.setCancelacionEvento(true); d.setEstado(EstadoDevolucion.APROBADA);
            d.setMotivoResolucion("Cancelacion de evento"); d.setFechaResolucion(LocalDateTime.now(clock)); ids.add(d.getId());
        }
        return List.copyOf(ids);
    }

    static void validarCompra(Compra compra, Long clienteId) {
        if (!Objects.equals(compra.getClienteId(), clienteId)) throw new IllegalArgumentException("La compra no pertenece al cliente");
        if (compra.getEstado() != EstadoCompra.COMPLETADA && compra.getEstado() != EstadoCompra.DEVUELTA_PARCIAL) throw new IllegalStateException("Solo se pueden devolver entradas de una compra completada");
    }

    private void validarEntradas(Compra compra, List<EntradaDTO> emitidas) {
        if (emitidas == null) throw new IllegalStateException("No se pudo verificar el estado de las entradas");
        Set<Long> ids = new HashSet<>();
        for (EntradaDTO e : emitidas) {
            if (e == null || e.entradaId() == null || e.entradaId() <= 0 || !ids.add(e.entradaId())
                    || !compra.getId().equals(e.compraId()) || !compra.getClienteId().equals(e.clienteId())
                    || !compra.getEventoId().equals(e.eventoId()))
                throw new IllegalStateException("La respuesta de Entradas no corresponde a la compra");
        }
        if (!ids.equals(new HashSet<>(compra.getEntradasIds())))
            throw new IllegalStateException("No se pudo verificar la totalidad de entradas de la compra");
    }

    private DetalleCompra asociarDetalle(Compra compra, EntradaDTO entrada, List<EntradaDTO> emitidas) {
        var candidatos = compra.getDetalles().stream().filter(d -> Objects.equals(d.getSectorId(), entrada.sectorId())
                && Objects.equals(d.getAsientoId(), entrada.asientoId())).sorted(Comparator.comparing(DetalleCompra::getId)).toList();
        if (candidatos.isEmpty()) throw new IllegalStateException("La entrada no coincide con un detalle historico");
        BigDecimal precio = candidatos.get(0).getPrecioUnitario();
        if (precio == null || precio.signum() <= 0 || candidatos.stream().anyMatch(d -> d.getPrecioUnitario() == null
                || precio.compareTo(d.getPrecioUnitario()) != 0))
            throw new IllegalStateException("La entrada tiene una asociacion ambigua de precios historicos");
        var grupo = emitidas.stream().filter(e -> Objects.equals(e.sectorId(), entrada.sectorId())
                && Objects.equals(e.asientoId(), entrada.asientoId())).map(EntradaDTO::entradaId).sorted().toList();
        long cantidad = candidatos.stream().mapToLong(DetalleCompra::getCantidad).sum();
        if (grupo.size() != cantidad || (entrada.asientoId() != null && cantidad != 1))
            throw new IllegalStateException("La cantidad de entradas no coincide con el detalle historico");
        int posicion = grupo.indexOf(entrada.entradaId());
        // Desempate estable entre detalles equivalentes, independiente de qué subset se solicite.
        for (DetalleCompra candidato : candidatos) {
            if (posicion < candidato.getCantidad()) return candidato;
            posicion -= candidato.getCantidad();
        }
        throw new IllegalStateException("No se pudo asociar la entrada al detalle historico");
    }

    public DevolucionDTO decidir(Long id, EstadoDevolucion destino, String motivo) {
        Devolucion d = devoluciones.buscarConBloqueo(id).orElseThrow(() -> new DevolucionNoEncontradaException(id));
        if (destino != EstadoDevolucion.APROBADA && destino != EstadoDevolucion.RECHAZADA)
            throw new IllegalArgumentException("Decision administrativa invalida");
        if (d.getEstado() == destino) {
            if (!Objects.equals(d.getMotivoResolucion(), motivo)) throw new IllegalStateException("La decision ya fue registrada con otro motivo");
            return convertir(d, detalles.findByDevolucionId(id));
        }
        if (d.getEstado() != EstadoDevolucion.PENDIENTE) throw new IllegalStateException("Solo se puede resolver una devolucion pendiente");
        d.setEstado(destino); d.setMotivoResolucion(motivo); d.setFechaResolucion(LocalDateTime.now(clock));
        if (destino == EstadoDevolucion.RECHAZADA) d.setMotivoRechazo(motivo);
        return convertir(d, detalles.findByDevolucionId(id));
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public DevolucionDTO obtener(Long id) {
        return convertir(devoluciones.findById(id).orElseThrow(() -> new DevolucionNoEncontradaException(id)), detalles.findByDevolucionId(id));
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<DevolucionDTO> obtenerPorEstado(EstadoDevolucion estado) {
        return devoluciones.findByEstado(estado).stream().map(d -> convertir(d, detalles.findByDevolucionId(d.getId()))).toList();
    }

    static DevolucionDTO convertir(Devolucion d, List<DetalleDevolucion> seleccion) {
        Compra c = d.getCompra();
        return new DevolucionDTO(d.getId(), c.getId(), c.getClienteId(), c.getEventoId(), d.getFechaSolicitud(), d.getEstado(),
                d.getMotivo(), d.getMotivoResolucion(), d.getMotivoRechazo(), d.getFechaResolucion(), d.getImporteSolicitado(),
                d.getImporteReintegrado(), seleccion.stream().map(t -> new DetalleDevolucionDTO(t.getId(), t.getDetalleCompra().getId(),
                        t.getEntradaId(), t.getImporte())).toList());
    }
}
