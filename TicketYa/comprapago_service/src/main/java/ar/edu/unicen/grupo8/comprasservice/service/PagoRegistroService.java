package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.dto.*;
import ar.edu.unicen.grupo8.comprasservice.entity.*;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.*;
import ar.edu.unicen.grupo8.comprasservice.exception.*;
import ar.edu.unicen.grupo8.comprasservice.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;
import java.util.function.BiConsumer;

/** Solo persistencia local. Nunca llama a servicios externos. */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class PagoRegistroService {
    public record Turno(Pago pago, String token, boolean creado) { }
    private final CompraRepository compras;
    private final PagoRepository pagos;
    private final ReintegroRepository reintegros;
    private final Clock clock;

    public PagoRegistroService(CompraRepository compras, PagoRepository pagos, ReintegroRepository reintegros, Clock clock) {
        this.compras = compras; this.pagos = pagos; this.reintegros = reintegros; this.clock = clock;
    }

    public Turno tomar(Long compraId, String clave, MedioPago medio) {
        Compra compra = compras.buscarPorIdConBloqueo(compraId).orElseThrow(() -> new CompraNoEncontradaException(compraId));
        Pago pago = pagos.findByClaveIdempotencia(clave).orElse(null);
        boolean creado = pago == null;
        if (pago != null && !pago.getCompra().getId().equals(compraId)) throw new IllegalStateException("La clave de idempotencia pertenece a otra compra");
        if (pago == null && medio == null) throw new IllegalArgumentException("El medio de pago es obligatorio para un pago nuevo");
        if (pago != null && pago.getMedioPago() != medio) {
            if (medio == null && pago.getMedioPago() != null) throw new IllegalArgumentException("Debe indicar el medio de pago original");
            throw new IllegalStateException("La clave de idempotencia fue registrada con otro medio de pago; un historico sin dato no puede completarse por reintento");
        }
        if (pago != null && compra.getEtapa() == EtapaCompra.FINALIZADA) return new Turno(inicializar(pago), null, false);
        if (compra.getEjecutor() != null && compra.getBloqueoHasta().isAfter(LocalDateTime.now(clock)))
            throw new IllegalStateException("La compra tiene una operacion en procesamiento; reintente con la misma clave");
        if (creado) {
            if (compra.getEstado() != EstadoCompra.PENDIENTE) throw new IllegalStateException("La compra debe estar pendiente para procesar un pago");
            if (!pagos.findByCompraId(compraId).isEmpty()) throw new IllegalStateException("La compra ya tiene un intento de pago; reutilice su clave");
            pago = new Pago();
            pago.setCompra(compra); pago.setClaveIdempotencia(clave); pago.setImporte(compra.getImporteTotal()); pago.setMedioPago(medio);
            pagos.saveAndFlush(pago);
            compra.setEstado(EstadoCompra.PROCESANDO);
        }
        String token = UUID.randomUUID().toString();
        compra.setEjecutor(token);
        compra.setBloqueoHasta(LocalDateTime.now(clock).plusMinutes(2));
        return new Turno(inicializar(pago), token, creado);
    }

    public Pago cambiar(Long id, String token, BiConsumer<Compra, Pago> cambio) {
        Pago pago = pagos.findById(id).orElseThrow();
        Compra compra = compras.buscarPorIdConBloqueo(pago.getCompra().getId()).orElseThrow();
        if (!Objects.equals(token, compra.getEjecutor())) throw new IllegalStateException("El turno fue recuperado por otra solicitud");
        cambio.accept(compra, pago);
        compra.setBloqueoHasta(LocalDateTime.now(clock).plusMinutes(2));
        return inicializar(pago);
    }

    public void soltar(Long compraId, String token) {
        Compra compra = compras.buscarPorIdConBloqueo(compraId).orElseThrow();
        if (Objects.equals(token, compra.getEjecutor())) { compra.setEjecutor(null); compra.setBloqueoHasta(null); }
    }

    public void prepararCompensacion(Long id, String token) {
        Pago pago = cambiar(id, token, (c, p) -> c.setEtapa(EtapaCompra.COMPENSACION));
        String clave = "compensacion:" + id;
        if (!reintegros.existsByClaveIdempotencia(clave)) {
            Reintegro reintegro = new Reintegro(); reintegro.setPago(pago);
            reintegro.setClaveIdempotencia(clave); reintegro.setImporte(pago.getImporte());
            reintegros.save(reintegro);
        }
    }

    public Pago registrarCompensacion(Long id, String token, ResultadoPasarela resultado) {
        return cambiar(id, token, (compra, pago) -> {
            Reintegro reintegro = reintegros.findByClaveIdempotencia("compensacion:" + id).orElseThrow();
            if (reintegro.getEstado() == EstadoReintegro.RECHAZADO) return;
            if (resultado == null) {
                if (pago.getEstado() == EstadoPago.APROBADO) pago.setEstado(EstadoPago.REINTEGRO_DESCONOCIDO);
                if (reintegro.getEstado() == EstadoReintegro.PENDIENTE) reintegro.setEstado(EstadoReintegro.RESULTADO_DESCONOCIDO);
            } else if (!resultado.isAprobado()) {
                reintegro.setEstado(EstadoReintegro.RECHAZADO);
                reintegro.setFechaProcesamiento(LocalDateTime.now(clock));
                // Rechazo conocido: el dinero sigue cobrado, no está reintegrado ni es desconocido.
                if (pago.getEstado() == EstadoPago.REINTEGRO_DESCONOCIDO) pago.setEstado(EstadoPago.APROBADO);
            } else {
                if (pago.getEstado() != EstadoPago.REINTEGRADO_TOTAL) pago.setEstado(EstadoPago.REINTEGRADO_TOTAL);
                pago.setImporteReintegrado(pago.getImporte()); pago.setReferenciaReintegro(resultado.getReferenciaExterna());
                if (reintegro.getEstado() != EstadoReintegro.COMPLETADO) reintegro.setEstado(EstadoReintegro.COMPLETADO);
                reintegro.setFechaProcesamiento(LocalDateTime.now(clock));
                compra.setEtapa(EtapaCompra.LIBERACION);
            }
        });
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public boolean compensacionRechazada(Long pagoId) {
        return reintegros.findByClaveIdempotencia("compensacion:" + pagoId)
                .map(r -> r.getEstado() == EstadoReintegro.RECHAZADO).orElse(false);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Pago obtener(Long id) {
        return inicializar(pagos.findById(id).orElseThrow(() -> new PagoNoEncontradoException("No existe un pago con ID: " + id)));
    }

    private Pago inicializar(Pago pago) {
        pago.getCompra().getDetalles().size(); pago.getCompra().getEntradasIds().size();
        return pago;
    }
}
