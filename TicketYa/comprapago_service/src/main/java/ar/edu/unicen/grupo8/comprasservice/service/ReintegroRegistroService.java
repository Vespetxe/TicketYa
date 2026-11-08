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
import java.util.function.BiConsumer;

/** Transacciones locales cortas. El bloqueo/turno de compra serializa todos sus reintegros. */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
public class ReintegroRegistroService {
    public record Turno(ResultadoDevolucionDTO resultado, Long compraId, Long reservaId,
            String clavePago, String token, Map<Long, DetalleEmisionDTO> lugares) { }
    private final CompraRepository compras;
    private final DevolucionRepository devoluciones;
    private final DetalleDevolucionRepository detalles;
    private final PagoRepository pagos;
    private final ReintegroRepository reintegros;
    private final Clock clock;
    public ReintegroRegistroService(CompraRepository compras, DevolucionRepository devoluciones,
            DetalleDevolucionRepository detalles, PagoRepository pagos, ReintegroRepository reintegros, Clock clock) {
        this.compras = compras; this.devoluciones = devoluciones; this.detalles = detalles;
        this.pagos = pagos; this.reintegros = reintegros; this.clock = clock;
    }
    private Devolucion buscar(Long id) {
        return devoluciones.findById(id).orElseThrow(() -> new DevolucionNoEncontradaException(id));
    }
    public Turno tomar(Long id, ProcesarDevolucionDTO dto) {
        Long compraId = devoluciones.buscarCompraId(id).orElseThrow(() -> new DevolucionNoEncontradaException(id));
        Compra c = compras.buscarPorIdConBloqueo(compraId).orElseThrow();
        Devolucion d = devoluciones.buscarConBloqueo(id).orElseThrow();
        Reintegro porClave = reintegros.findByClaveIdempotencia(dto.claveIdempotencia()).orElse(null);
        if (porClave != null && (porClave.getDevolucion() == null || !id.equals(porClave.getDevolucion().getId())
                || porClave.getImporte().compareTo(dto.importe()) != 0)) throw new IllegalStateException("Clave usada con otros datos");
        if (dto.importe().compareTo(d.getImporteSolicitado()) != 0)
            throw new IllegalArgumentException("Debe reintegrarse el importe completo autorizado para esta devolucion");
        var anteriores = reintegros.findByDevolucionId(id);
        if (!anteriores.isEmpty() && porClave == null) throw new IllegalStateException("Reintente con la clave original de la devolucion");
        if (d.getEstado() == EstadoDevolucion.COMPLETADA) return turno(d, porClave, null);
        if (d.getEstado() != EstadoDevolucion.APROBADA && d.getEstado() != EstadoDevolucion.ERROR_PENDIENTE
                && d.getEstado() != EstadoDevolucion.PROCESANDO) throw new IllegalStateException("La devolucion no esta aprobada");
        if (c.getEjecutor() != null && c.getBloqueoHasta() != null && c.getBloqueoHasta().isAfter(LocalDateTime.now(clock)))
            throw new IllegalStateException("La compra tiene una operacion en curso; reintente con la misma clave");
        if (porClave == null) {
            DevolucionRegistroService.validarCompra(c, c.getClienteId());
            var candidatos = pagos.findByCompraId(compraId).stream().filter(p -> p.getEstado() == EstadoPago.APROBADO
                    || p.getEstado() == EstadoPago.REINTEGRADO_PARCIAL).toList();
            if (candidatos.size() != 1) throw new IllegalStateException("No existe un pago reintegrable inequivoco");
            Pago p = candidatos.get(0);
            BigDecimal comprometido = reintegros.findByPagoId(p.getId()).stream()
                    .filter(r -> r.getEstado() != EstadoReintegro.RECHAZADO).map(Reintegro::getImporte)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (comprometido.add(dto.importe()).compareTo(p.getImporte()) > 0
                    || p.getImporteReintegrado().add(dto.importe()).compareTo(p.getImporte()) > 0)
                throw new IllegalStateException("El reintegro supera el saldo efectivamente pagado");
            porClave = new Reintegro(); porClave.setPago(p); porClave.setDevolucion(d);
            porClave.setClaveIdempotencia(dto.claveIdempotencia()); porClave.setImporte(dto.importe());
            reintegros.saveAndFlush(porClave);
        }
        // Las solicitudes de 6.1–6.4 todavía no tenían una etapa de procesamiento.
        if (d.getEtapa() == null) d.setEtapa(EtapaDevolucion.REINTEGRO);
        if (porClave.getEstado() == EstadoReintegro.RECHAZADO)
            throw new IllegalStateException("Reintegro rechazado definitivamente; requiere resolucion administrativa");
        if (d.getEstado() != EstadoDevolucion.PROCESANDO) d.setEstado(EstadoDevolucion.PROCESANDO);
        String token = UUID.randomUUID().toString(); c.setEjecutor(token); c.setBloqueoHasta(LocalDateTime.now(clock).plusMinutes(2));
        return turno(d, porClave, token);
    }
    private Turno turno(Devolucion d, Reintegro r, String token) {
        Map<Long, DetalleEmisionDTO> lugares = new HashMap<>();
        for (var t : detalles.findByDevolucionId(d.getId()))
            lugares.put(t.getEntradaId(), new DetalleEmisionDTO(t.getDetalleCompra().getSectorId(), t.getDetalleCompra().getAsientoId(), 1));
        return new Turno(resultado(d, r), d.getCompra().getId(), d.getCompra().getReservaId(),
                r.getPago().getClaveIdempotencia(), token, Map.copyOf(lugares));
    }
    private ResultadoDevolucionDTO resultado(Devolucion d, Reintegro r) {
        return new ResultadoDevolucionDTO(DevolucionRegistroService.convertir(d, detalles.findByDevolucionId(d.getId())),
                convertir(r), d.getEtapa(), d.isCancelacionEvento());
    }
    static ReintegroDTO convertir(Reintegro r) {
        return new ReintegroDTO(r.getId(), r.getPago().getId(), r.getDevolucion() == null ? null : r.getDevolucion().getId(),
                r.getClaveIdempotencia(), r.getImporte(), r.getEstado(), r.getFechaProcesamiento(), r.getReferenciaExterna());
    }
    private Turno cambiar(Long id, String token, BiConsumer<Devolucion, Reintegro> accion) {
        Long compraId = devoluciones.buscarCompraId(id).orElseThrow(() -> new DevolucionNoEncontradaException(id));
        var c = compras.buscarPorIdConBloqueo(compraId).orElseThrow();
        if (!Objects.equals(token, c.getEjecutor())) throw new IllegalStateException("El turno de procesamiento ya no pertenece a esta operacion");
        var d = devoluciones.buscarConBloqueo(id).orElseThrow();
        var r = reintegros.findByDevolucionId(id).get(0);
        accion.accept(d, r); c.setBloqueoHasta(LocalDateTime.now(clock).plusMinutes(2));
        return turno(d, r, token);
    }
    public Turno registrarResultado(Long id, String token, ResultadoPasarela respuesta) {
        return cambiar(id, token, (d, r) -> {
            if (r.getEstado() == EstadoReintegro.COMPLETADO) return;
            if (respuesta == null) {
                if (r.getEstado() == EstadoReintegro.PENDIENTE) r.setEstado(EstadoReintegro.RESULTADO_DESCONOCIDO);
                return;
            }
            r.setEstado(respuesta.isAprobado() ? EstadoReintegro.COMPLETADO : EstadoReintegro.RECHAZADO);
            r.setFechaProcesamiento(LocalDateTime.now(clock)); r.setReferenciaExterna(respuesta.getReferenciaExterna());
            if (!respuesta.isAprobado()) return;
            Pago p = r.getPago();
            p.setImporteReintegrado(p.getImporteReintegrado().add(r.getImporte()));
            EstadoPago nuevo = p.getImporteReintegrado().compareTo(p.getImporte()) == 0 ? EstadoPago.REINTEGRADO_TOTAL : EstadoPago.REINTEGRADO_PARCIAL;
            if (p.getEstado() != nuevo) p.setEstado(nuevo);
            p.setReferenciaReintegro(respuesta.getReferenciaExterna());
            d.setImporteReintegrado(r.getImporte()); d.setEtapa(EtapaDevolucion.ANULACION);
            var c = d.getCompra();
            EstadoCompra estado = p.getImporteReintegrado().compareTo(c.getImporteTotal()) == 0 ? EstadoCompra.DEVUELTA_TOTAL : EstadoCompra.DEVUELTA_PARCIAL;
            if (c.getEstado() != estado) c.setEstado(estado);
        });
    }
    public Turno avanzar(Long id, String token, EtapaDevolucion etapa) {
        return cambiar(id, token, (d, r) -> {
            d.setEtapa(etapa);
            if (etapa == EtapaDevolucion.FINALIZADA) d.setEstado(EstadoDevolucion.COMPLETADA);
        });
    }
    public void error(Long id, String token) {
        cambiar(id, token, (d, r) -> {
            if (d.getEstado() == EstadoDevolucion.PROCESANDO) d.setEstado(EstadoDevolucion.ERROR_PENDIENTE);
        });
    }
    public void soltar(Long compraId, String token) {
        var c = compras.buscarPorIdConBloqueo(compraId).orElseThrow();
        if (Objects.equals(token, c.getEjecutor())) { c.setEjecutor(null); c.setBloqueoHasta(null); }
    }
    @Transactional(readOnly=true, propagation=Propagation.REQUIRES_NEW)
    public ResultadoDevolucionDTO obtener(Long id) {
        var d = buscar(id);
        var r = reintegros.findByDevolucionId(id);
        if (r.isEmpty()) throw new IllegalStateException("La devolucion todavia no tiene reintegro");
        return resultado(d, r.get(0));
    }
    @Transactional(readOnly=true, propagation=Propagation.REQUIRES_NEW)
    public ReintegroDTO obtenerReintegro(Long id) {
        return convertir(reintegros.findById(id).orElseThrow(() -> new ReintegroNoEncontradoException(id)));
    }
    @Transactional(readOnly=true, propagation=Propagation.REQUIRES_NEW)
    public List<Long> comprasDelEvento(Long eventoId) {
        return compras.findByEventoId(eventoId).stream().filter(c -> c.getEstado() == EstadoCompra.COMPLETADA
                || c.getEstado() == EstadoCompra.DEVUELTA_PARCIAL || c.getEstado() == EstadoCompra.DEVUELTA_TOTAL
                || c.getEstado() == EstadoCompra.PENDIENTE || c.getEstado() == EstadoCompra.PROCESANDO
                || (c.getEstado() == EstadoCompra.CANCELADA && c.getEtapa() == EtapaCompra.LIBERACION))
                .map(Compra::getId).sorted().toList();
    }
    @Transactional(readOnly=true, propagation=Propagation.REQUIRES_NEW)
    public ProcesarDevolucionDTO datosParaCancelacion(Long id) {
        var d = buscar(id); var rs = reintegros.findByDevolucionId(id);
        return new ProcesarDevolucionDTO(rs.isEmpty() ? "cancelacion-devolucion:" + id : rs.get(0).getClaveIdempotencia(), d.getImporteSolicitado());
    }
}
