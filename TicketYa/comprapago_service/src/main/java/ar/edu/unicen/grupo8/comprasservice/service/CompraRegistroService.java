package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.entity.Compra;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoCompra;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EtapaCompra;
import ar.edu.unicen.grupo8.comprasservice.exception.CompraNoEncontradaException;
import ar.edu.unicen.grupo8.comprasservice.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.List;

@Service
public class CompraRegistroService {
    private final CompraRepository compras;
    private final ControlLimiteEntradasRepository limites;
    public CompraRegistroService(CompraRepository compras, ControlLimiteEntradasRepository limites) {
        this.compras = compras;
        this.limites = limites;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Compra registrar(Compra compra, int limite) {
        limites.asegurarRegistro(compra.getClienteId(), compra.getEventoId());
        limites.buscarConBloqueo(compra.getClienteId(), compra.getEventoId())
                .orElseThrow(() -> new IllegalStateException("No se pudo obtener el control de limite de entradas"));
        if (compras.findByReservaId(compra.getReservaId()).isPresent()) {
            throw new IllegalStateException("La reserva ya tiene una compra asociada");
        }
        // Las reservas temporales vencidas no deben consumir indefinidamente el cupo comercial.
        var ahora = java.time.LocalDateTime.now(java.time.Clock.systemUTC());
        compras.findByClienteIdAndEventoIdAndEstado(compra.getClienteId(), compra.getEventoId(), EstadoCompra.PENDIENTE)
                .stream().filter(c -> c.getReservaVencimiento() != null && !c.getReservaVencimiento().isAfter(ahora))
                .forEach(c -> { c.setEstado(EstadoCompra.FALLIDA); c.setEtapa(ar.edu.unicen.grupo8.comprasservice.entity.enums.EtapaCompra.FINALIZADA); });
        var estados = List.of(EstadoCompra.PENDIENTE, EstadoCompra.PROCESANDO, EstadoCompra.COMPLETADA, EstadoCompra.DEVUELTA_PARCIAL);
        long existentes = compras.contarEntradasPorClienteYEvento(compra.getClienteId(), compra.getEventoId(), estados);
        // Solo un reintegro confirmado devuelve cupo; ni una aprobación ni un resultado incierto lo hacen.
        existentes -= compras.contarEntradasReintegradas(compra.getClienteId(), compra.getEventoId(), estados,
                ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoReintegro.COMPLETADO);
        long solicitadas = compra.getDetalles().stream().mapToLong(d -> d.getCantidad()).sum();
        if (existentes + solicitadas > limite) {
            throw new IllegalArgumentException("La compra supera el limite de entradas permitido por cliente y evento");
        }
        return compras.save(compra);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Compra solicitarCancelacion(Long id) {
        Compra compra = compras.buscarPorIdConBloqueo(id).orElseThrow(() -> new CompraNoEncontradaException(id));
        if (compra.getEstado() == EstadoCompra.PENDIENTE) {
            compra.setEstado(EstadoCompra.CANCELADA);
            compra.setEtapa(EtapaCompra.LIBERACION);
        } else if (compra.getEstado() != EstadoCompra.CANCELADA) {
            throw new IllegalStateException("Solo se puede cancelar una compra pendiente de pago");
        }
        return inicializar(compra);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Compra finalizarCancelacion(Long id) {
        Compra compra = compras.buscarPorIdConBloqueo(id).orElseThrow(() -> new CompraNoEncontradaException(id));
        if (compra.getEstado() != EstadoCompra.CANCELADA) throw new IllegalStateException("La compra no tiene una cancelacion en curso");
        compra.setEtapa(EtapaCompra.FINALIZADA);
        return inicializar(compra);
    }

    private Compra inicializar(Compra compra) {
        compra.getDetalles().size(); compra.getEntradasIds().size();
        return compra;
    }
}
