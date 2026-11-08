package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.client.*;
import ar.edu.unicen.grupo8.comprasservice.dto.*;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.*;
import ar.edu.unicen.grupo8.comprasservice.exception.ComunicacionPasarelaException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.util.*;

/** Orquestación recuperable. Ninguna llamada externa comparte la transacción local. */
@Service
@Validated
@Transactional(propagation=Propagation.NEVER)
public class ReintegroService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReintegroService.class);
    private final ReintegroRegistroService registro;
    private final DevolucionRegistroService devoluciones;
    private final PasarelaPagoService pasarela;
    private final EntradaClient entradas;
    private final ReservaClient reservas;
    private final EventoClient eventos;
    private final CompraService compras;
    private final java.time.Clock clock;
    public ReintegroService(ReintegroRegistroService registro, DevolucionRegistroService devoluciones,
            PasarelaPagoService pasarela, EntradaClient entradas, ReservaClient reservas, EventoClient eventos,
            CompraService compras, java.time.Clock clock) {
        this.registro = registro; this.devoluciones = devoluciones; this.pasarela = pasarela;
        this.entradas = entradas; this.reservas = reservas; this.eventos = eventos; this.compras = compras; this.clock = clock;
    }
    public ResultadoDevolucionDTO procesar(@NotNull @Positive Long id, @NotNull @Valid ProcesarDevolucionDTO dto) {
        var turno = registro.tomar(id, dto);
        if (turno.token() == null) return turno.resultado();
        String token = turno.token(); Long compraId = turno.compraId();
        try {
            if (turno.resultado().etapa() == EtapaDevolucion.REINTEGRO) {
                ResultadoPasarela respuesta;
                try {
                    // Siempre consultar primero: una respuesta perdida no habilita otra identidad de operación.
                    respuesta = pasarela.consultarReintegro(turno.clavePago(), dto.claveIdempotencia());
                    if (respuesta == null) respuesta = pasarela.reintegrarPago(turno.clavePago(), dto.claveIdempotencia(), dto.importe());
                } catch (RuntimeException ex) {
                    registro.registrarResultado(id, token, null);
                    throw ex;
                }
                turno = registro.registrarResultado(id, token, respuesta);
                if (respuesta == null) throw new ComunicacionPasarelaException("No se conoce el resultado del reintegro");
                if (!respuesta.isAprobado()) {
                    registro.error(id, token);
                    throw new IllegalStateException("El reintegro fue rechazado; requiere resolucion administrativa");
                }
            }
            var ids = turno.resultado().devolucion().detalles().stream().map(DetalleDevolucionDTO::entradaId).toList();
            if (turno.resultado().etapa() == EtapaDevolucion.ANULACION) {
                var tickets = seleccionConfirmada(turno, ids);
                if (tickets.stream().anyMatch(e -> !"ANULADA".equals(e.estado()))) {
                    entradas.anularEntradas(compraId, ids);
                    tickets = seleccionConfirmada(turno, ids);
                    if (tickets.stream().anyMatch(e -> !"ANULADA".equals(e.estado())))
                        throw new ComunicacionPasarelaException("Entradas no confirmo la anulacion");
                }
                turno = registro.avanzar(id, token, EtapaDevolucion.LIBERACION);
            }
            if (turno.resultado().etapa() == EtapaDevolucion.LIBERACION) {
                // El contrato real deberá verificar también en Reservas la disponibilidad del evento.
                if (!turno.resultado().cancelacionEvento() && eventos.eventoValido(turno.resultado().devolucion().eventoId())
                        && eventos.obtenerFechaInicio(turno.resultado().devolucion().eventoId()).isAfter(java.time.LocalDateTime.now(clock))) {
                    var tickets = seleccionConfirmada(turno, ids);
                    if (tickets.stream().anyMatch(e -> !"ANULADA".equals(e.estado())))
                        throw new ComunicacionPasarelaException("No se pueden liberar entradas vigentes");
                    if (!reservas.consultarLugaresLiberados(turno.reservaId()).containsAll(ids)) {
                        reservas.liberarLugares(turno.reservaId(), tickets);
                        if (!reservas.consultarLugaresLiberados(turno.reservaId()).containsAll(ids))
                            throw new ComunicacionPasarelaException("Reservas no confirmo la liberacion");
                    }
                }
                turno = registro.avanzar(id, token, EtapaDevolucion.FINALIZADA);
            }
            return turno.resultado();
        } catch (IllegalStateException ex) {
            registro.error(id, token); throw ex;
        } catch (RuntimeException ex) {
            registro.error(id, token);
            log.warn("Devolucion {} pendiente de recuperacion: {}", id, ex.toString());
            throw new ComunicacionPasarelaException("La devolucion quedo pendiente; reintente con la misma clave");
        } finally { registro.soltar(compraId, token); }
    }
    private List<EntradaDTO> seleccionConfirmada(ReintegroRegistroService.Turno turno, List<Long> ids) {
        var todos = entradas.consultarEntradas(turno.compraId());
        if (todos == null) throw new ComunicacionPasarelaException("No se pudieron consultar las entradas");
        var seleccion = todos.stream().filter(e -> e != null && ids.contains(e.entradaId())).toList();
        var d = turno.resultado().devolucion();
        if (seleccion.size() != ids.size() || seleccion.stream().map(EntradaDTO::entradaId).distinct().count() != ids.size()
                || seleccion.stream().anyMatch(e -> !turno.compraId().equals(e.compraId()) || !d.clienteId().equals(e.clienteId())
                    || !d.eventoId().equals(e.eventoId()) || !Objects.equals(turno.lugares().get(e.entradaId()).sectorId(), e.sectorId())
                    || !Objects.equals(turno.lugares().get(e.entradaId()).asientoId(), e.asientoId())))
            throw new ComunicacionPasarelaException("La respuesta de Entradas no corresponde a la devolucion");
        return seleccion;
    }
    public ResultadoDevolucionDTO obtener(@NotNull @Positive Long id) { return registro.obtener(id); }
    public ReintegroDTO obtenerReintegro(@NotNull @Positive Long id) { return registro.obtenerReintegro(id); }

    /** Notificación administrativa simulada; repetir la notificación recupera las compras pendientes. */
    public CancelacionEventoDTO cancelarEvento(@NotNull @Positive Long eventoId) {
        try { eventos.registrarCancelacion(eventoId); }
        catch (IllegalArgumentException ex) { throw new ar.edu.unicen.grupo8.comprasservice.exception.EventoNoEncontradoException(eventoId); }
        List<ResultadoDevolucionDTO> resultados = new ArrayList<>();
        List<Long> pendientes = new ArrayList<>();
        for (Long compraId : registro.comprasDelEvento(eventoId)) {
            try {
                var compra = compras.obtenerCompraPorId(compraId);
                if (compra.getEstado() == EstadoCompra.PROCESANDO) {
                    // Primero resolver el pago existente; repetir esta notificación recogerá su resultado.
                    pendientes.add(compraId); continue;
                }
                if (compra.getEstado() == EstadoCompra.PENDIENTE || compra.getEstado() == EstadoCompra.CANCELADA) {
                    var cancelada = compras.cancelarCompra(compraId);
                    if (cancelada.getEtapa() != EtapaCompra.FINALIZADA) pendientes.add(compraId);
                    continue;
                }
                var ids = devoluciones.prepararCancelacion(compraId, entradas.consultarEntradas(compraId));
                for (Long id : ids) {
                    try { resultados.add(procesar(id, registro.datosParaCancelacion(id))); }
                    catch (RuntimeException ex) {
                        if (!pendientes.contains(compraId)) pendientes.add(compraId);
                        log.warn("Cancelacion evento {}, devolucion {} pendiente: {}", eventoId, id, ex.toString());
                    }
                }
            } catch (RuntimeException ex) {
                if (!pendientes.contains(compraId)) pendientes.add(compraId);
                log.warn("Cancelacion evento {}, compra {} pendiente: {}", eventoId, compraId, ex.toString());
            }
        }
        return new CancelacionEventoDTO(eventoId, resultados, pendientes);
    }
}
