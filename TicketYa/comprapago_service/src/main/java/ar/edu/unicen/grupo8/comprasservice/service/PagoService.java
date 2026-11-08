package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.client.*;
import ar.edu.unicen.grupo8.comprasservice.dto.*;
import ar.edu.unicen.grupo8.comprasservice.entity.*;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.*;
import ar.edu.unicen.grupo8.comprasservice.exception.*;
import ar.edu.unicen.grupo8.comprasservice.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

@Service
public class PagoService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PagoService.class);
    private final ReservaClient reservas;
    private final EntradaClient entradas;
    private final PasarelaPagoService pasarela;
    private final PagoRegistroService registro;
    private final CompraService compras;
    private final PagoRepository pagos;
    private final Clock clock;

    public PagoService(ReservaClient reservas, EntradaClient entradas, PasarelaPagoService pasarela,
            PagoRegistroService registro, CompraService compras, PagoRepository pagos, Clock clock) {
        this.reservas = reservas; this.entradas = entradas; this.pasarela = pasarela;
        this.registro = registro; this.compras = compras; this.pagos = pagos; this.clock = clock;
    }

    public void validarCondicionesPrevias(Compra compra) {
        if (compra == null) throw new IllegalArgumentException("La compra no puede ser nula");
        if (compra.getEstado() != EstadoCompra.PENDIENTE)
            throw new IllegalArgumentException("La compra debe estar pendiente para procesar un pago");
        if (!reservas.reservaValida(compra.getReservaId(), compra.getClienteId(), compra.getEventoId()))
            throw new IllegalArgumentException("La reserva no es valida, esta vencida o no corresponde a la compra");
        SeleccionReserva.validar(reservas.consultarReserva(compra.getReservaId()), detalles(compra));
    }

    @Transactional(propagation = Propagation.NEVER)
    public Pago procesarPago(Long compraId, String clave, MedioPago medio) { return procesarPagoConResultado(compraId, clave, medio).pago(); }

    @Transactional(propagation = Propagation.NEVER)
    public ResultadoProcesamientoPagoDTO procesarPagoConResultado(Long compraId, String clave, MedioPago medio) {
        validarId(compraId);
        if (clave == null || clave.isBlank() || clave.length() > 100)
            throw new IllegalArgumentException("La clave de idempotencia es obligatoria y admite hasta 100 caracteres");
        // El dato ausente solo es compatible con una operación histórica ya registrada.
        if (medio == null) {
            var existente = pagos.findByClaveIdempotencia(clave);
            if (existente.isEmpty() || existente.get().getMedioPago() != null)
                throw new IllegalArgumentException("El medio de pago es obligatorio; omitirlo solo para recuperar historicos sin dato");
        }
        // La validación remota ocurre antes de abrir la transacción que reclama la compra.
        Compra compra = compras.obtenerCompraPorId(compraId);
        if (compra.getEstado() == EstadoCompra.PENDIENTE && pagos.findByClaveIdempotencia(clave).isEmpty())
            validarCondicionesPrevias(compra);
        var turno = registro.tomar(compraId, clave, medio);
        if (turno.token() == null) return new ResultadoProcesamientoPagoDTO(turno.pago(), false);
        Pago pago = turno.pago();
        try {
            continuar(pago, turno.token(), turno.creado());
        } finally {
            registro.soltar(compraId, turno.token());
        }
        return new ResultadoProcesamientoPagoDTO(registro.obtener(pago.getId()), turno.creado());
    }

    private void continuar(Pago inicial, String token, boolean nuevo) {
        Pago pago = inicial;
        // Como máximo una pasada por cada etapa; una respuesta incierta queda recuperable.
        for (int paso = 0; paso < 6; paso++) {
            Compra compra = pago.getCompra();
            EtapaCompra etapa = compra.getEtapa();
            if (etapa == EtapaCompra.FINALIZADA) return;
            switch (etapa) {
                case PAGO -> {
                    ResultadoPasarela resultado;
                    try {
                        resultado = nuevo ? pasarela.procesarPago(pago.getClaveIdempotencia(), pago.getImporte())
                                : pasarela.consultarPago(pago.getClaveIdempotencia());
                        if (resultado == null && pago.getEstado() == EstadoPago.PENDIENTE)
                            resultado = pasarela.procesarPago(pago.getClaveIdempotencia(), pago.getImporte());
                    } catch (RuntimeException ex) {
                        pendiente(compra, ex);
                        registro.cambiar(pago.getId(), token, (c, p) -> {
                            if (p.getEstado() == EstadoPago.PENDIENTE) p.setEstado(EstadoPago.RESULTADO_DESCONOCIDO);
                            p.setFechaProcesamiento(LocalDateTime.now(clock));
                        });
                        return;
                    }
                    if (resultado == null) return;
                    ResultadoPasarela definitivo = resultado;
                    pago = registro.cambiar(pago.getId(), token, (c, p) -> {
                        p.setEstado(definitivo.isAprobado() ? EstadoPago.APROBADO : EstadoPago.RECHAZADO);
                        p.setReferenciaExterna(definitivo.getReferenciaExterna()); p.setFechaProcesamiento(LocalDateTime.now(clock));
                        c.setEtapa(definitivo.isAprobado() ? EtapaCompra.CONFIRMACION : EtapaCompra.LIBERACION);
                    });
                }
                case CONFIRMACION -> {
                    try {
                        ReservaDTO reserva = reservas.consultarReserva(compra.getReservaId());
                        validarReserva(compra, reserva);
                        if (!"CONFIRMADA".equals(reserva.estado())) {
                            reservas.confirmarReserva(compra.getReservaId());
                            reserva = reservas.consultarReserva(compra.getReservaId());
                            validarReserva(compra, reserva);
                            if (!"CONFIRMADA".equals(reserva.estado())) return;
                        }
                    } catch (OperacionExternaRechazadaException ex) {
                        registro.prepararCompensacion(pago.getId(), token);
                        pago = registro.obtener(pago.getId());
                        continue;
                    } catch (RuntimeException ex) { pendiente(compra, ex); return; }
                    pago = registro.cambiar(pago.getId(), token, (c, p) -> c.setEtapa(EtapaCompra.EMISION));
                }
                case EMISION -> {
                    List<EntradaDTO> emitidas;
                    try {
                        emitidas = entradas.consultarEntradas(compra.getId());
                        if (emitidas == null) return; // null no significa ausencia confirmada
                        if (emitidas.isEmpty()) emitidas = entradas.solicitarEntradas(compra.getId(), compra.getEventoId(), compra.getClienteId(), detalles(compra));
                        validarEmision(compra, emitidas);
                    } catch (OperacionExternaRechazadaException ex) {
                        registro.prepararCompensacion(pago.getId(), token);
                        pago = registro.obtener(pago.getId());
                        continue;
                    } catch (RuntimeException ex) { pendiente(compra, ex); return; }
                    List<EntradaDTO> resultado = emitidas;
                    pago = registro.cambiar(pago.getId(), token, (c, p) -> {
                        c.getEntradasIds().clear(); c.getEntradasIds().addAll(resultado.stream().map(EntradaDTO::entradaId).toList());
                        c.setEstado(EstadoCompra.COMPLETADA); c.setEtapa(EtapaCompra.FINALIZADA);
                    });
                }
                case COMPENSACION -> {
                    // Intención durable antes de anular/reintegrar. Repetir conserva las mismas claves.
                    registro.prepararCompensacion(pago.getId(), token);
                    verificarCompensacionNoRechazada(pago);
                    try {
                        entradas.anularEntradas(compra.getId());
                        var existentes = entradas.consultarEntradas(compra.getId());
                        if (existentes == null || existentes.stream().anyMatch(e -> !"ANULADA".equals(e.estado()))) return;
                    } catch (RuntimeException ex) { pendiente(compra, ex); return; }
                    try {
                        ResultadoPasarela reintegro = pasarela.consultarReintegro(pago.getClaveIdempotencia());
                        if (reintegro == null) reintegro = pasarela.reintegrarPago(pago.getClaveIdempotencia(), pago.getImporte());
                        pago = registro.registrarCompensacion(pago.getId(), token, reintegro);
                    } catch (RuntimeException ex) {
                        pendiente(compra, ex);
                        registro.registrarCompensacion(pago.getId(), token, null);
                        return;
                    }
                    verificarCompensacionNoRechazada(pago);
                    if (pago.getCompra().getEtapa() == EtapaCompra.COMPENSACION) return;
                }
                case LIBERACION -> {
                    try {
                        if (pago.getEstado() == EstadoPago.RECHAZADO) reservas.liberarReserva(compra.getReservaId());
                        else reservas.compensarReserva(compra.getReservaId());
                        if (!"LIBERADA".equals(reservas.consultarReserva(compra.getReservaId()).estado())) return;
                    } catch (RuntimeException ex) { pendiente(compra, ex); return; }
                    pago = registro.cambiar(pago.getId(), token, (c, p) -> {
                        c.setEstado(EstadoCompra.FALLIDA); c.setEtapa(EtapaCompra.FINALIZADA);
                    });
                }
                default -> throw new IllegalStateException("Etapa desconocida");
            }
        }
    }

    private void validarReserva(Compra c, ReservaDTO r) {
        if (r == null) throw new IllegalStateException("Resultado de reserva desconocido");
        if (!c.getReservaId().equals(r.reservaId()) || !c.getClienteId().equals(r.clienteId()) || !c.getEventoId().equals(r.eventoId()))
            throw new OperacionExternaRechazadaException("La reserva no corresponde a la compra");
        SeleccionReserva.validar(r, detalles(c));
        if (!"CONFIRMADA".equals(r.estado()) && (!"TEMPORAL".equals(r.estado()) || !r.fechaVencimiento().isAfter(LocalDateTime.now(clock))))
            throw new OperacionExternaRechazadaException("La reserva ya no puede confirmarse");
    }
    private void validarEmision(Compra c, List<EntradaDTO> entradas) {
        if (entradas == null || entradas.isEmpty() || entradas.stream().anyMatch(e -> e == null || e.entradaId() == null
                || !c.getId().equals(e.compraId()) || !c.getClienteId().equals(e.clienteId())
                || !c.getEventoId().equals(e.eventoId()) || !"EMITIDA".equals(e.estado()))
                || entradas.stream().map(EntradaDTO::entradaId).distinct().count() != entradas.size())
            throw new IllegalStateException("Respuesta de emision incompleta o incompatible");
        var seleccion = entradas.stream().map(e -> new DetalleEmisionDTO(e.sectorId(), e.asientoId(), 1)).toList();
        if (!SeleccionReserva.agrupar(seleccion).equals(SeleccionReserva.agrupar(detalles(c))))
            throw new IllegalStateException("Las entradas emitidas no corresponden a la compra");
    }
    private List<DetalleEmisionDTO> detalles(Compra c) {
        return c.getDetalles().stream().map(d -> new DetalleEmisionDTO(d.getSectorId(), d.getAsientoId(), d.getCantidad())).toList();
    }
    public Pago obtenerPagoPorId(Long id) { validarId(id); return registro.obtener(id); }
    public List<Pago> obtenerPagosPorCompra(Long id) {
        validarId(id); return pagos.findByCompraId(id).stream().map(p -> registro.obtener(p.getId())).toList();
    }
    private void validarId(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("El identificador debe ser positivo");
    }
    private void pendiente(Compra compra, RuntimeException ex) {
        log.warn("Compra {} pendiente de recuperacion en etapa {} ({})", compra.getId(), compra.getEtapa(), ex.getClass().getSimpleName());
    }
    private void verificarCompensacionNoRechazada(Pago pago) {
        if (registro.compensacionRechazada(pago.getId())) {
            throw new IllegalStateException("El reintegro de compensacion fue rechazado; requiere resolucion manual. No se iniciara otro cobro ni reintegro automatico");
        }
    }
}
