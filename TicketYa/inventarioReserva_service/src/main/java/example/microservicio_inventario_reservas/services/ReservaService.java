package example.microservicio_inventario_reservas.services;

import example.microservicio_inventario_reservas.dtos.CrearReservaRequest;
import example.microservicio_inventario_reservas.dtos.DetalleAsientoResponse;
import example.microservicio_inventario_reservas.dtos.MensajeResponse;
import example.microservicio_inventario_reservas.dtos.ReservaResponse;
import example.microservicio_inventario_reservas.entities.Asiento;
import example.microservicio_inventario_reservas.entities.DetalleReserva;
import example.microservicio_inventario_reservas.entities.InventarioSector;
import example.microservicio_inventario_reservas.entities.Reserva;
import example.microservicio_inventario_reservas.enums.EstadoAsiento;
import example.microservicio_inventario_reservas.enums.EstadoInventario;
import example.microservicio_inventario_reservas.enums.EstadoReserva;
import example.microservicio_inventario_reservas.repositories.AsientoRepository;
import example.microservicio_inventario_reservas.repositories.InventarioSectorRepository;
import example.microservicio_inventario_reservas.repositories.ReservaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service("ReservaService")
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private InventarioSectorRepository inventarioSectorRepository;
    @Autowired
    private AsientoRepository asientoRepository;
    @Autowired
    private example.microservicio_inventario_reservas.repositories.LugarDevueltoRepository devoluciones;

    @org.springframework.beans.factory.annotation.Value("${reservas.ttl-seconds:600}")
    private long ttlSeconds=600;

    // 1. POST /reservas (Crear reserva temporal - Anti Doble Venta)
    @Transactional
    public ReservaResponse crearReservaTemporal(CrearReservaRequest request) {
        if (request == null || request.getCantidad() == null || request.getCantidad() <= 0
                || request.getIdCliente() == null || request.getIdCliente() <= 0
                || request.getIdEvento() == null || request.getIdEvento() <= 0
                || request.getIdSector() == null || request.getIdSector() <= 0) {
            throw new IllegalArgumentException("Cliente, evento, sector y cantidad deben ser positivos");
        }
        // Bloqueo pesimista sobre la fila del inventario del sector
        InventarioSector inventario = inventarioSectorRepository
                .findByIdEventoAndIdSectorWithLock(request.getIdEvento(), request.getIdSector())
                .orElseThrow(() -> new example.microservicio_inventario_reservas.exceptions.RecursoNoEncontradoException("Inventario no encontrado"));

        if (inventario.getEstado() != EstadoInventario.HABILITADO) {
            throw new IllegalStateException("El evento o sector se encuentra cancelado");
        }

        if (inventario.getDisponibles() < request.getCantidad()) {
            throw new IllegalStateException("Capacidad o aforo insuficiente");
        }

        if (request.getIdCliente() == null) {
            throw new IllegalArgumentException("No se ingreso el cliente");
        }

        Reserva reserva = new Reserva();
        reserva.setIdCliente(request.getIdCliente());
        reserva.setIdEvento(request.getIdEvento());
        reserva.setIdSector(request.getIdSector());
        reserva.setCantidad(request.getCantidad());
        reserva.setFechaCreacion(LocalDateTime.now(java.time.ZoneOffset.UTC));
        reserva.setFechaExpiracion(LocalDateTime.now(java.time.ZoneOffset.UTC).plusSeconds(ttlSeconds));
        reserva.setEstado(EstadoReserva.PENDIENTE);

        List<DetalleAsientoResponse> asientosReservadosDTO = new ArrayList<>();

        if (inventario.isEsNumerado()) {
            if (request.getIdsAsientos() == null || request.getIdsAsientos().size() != request.getCantidad()) {
                throw new IllegalArgumentException("Debe especificar la lista exacta de asientos numerados");
            }

            // Bloqueo pesimista sobre los asientos seleccionados
            List<Asiento> asientos = asientoRepository.findByIdsWithLock(request.getIdsAsientos(), inventario.getIdInventarioSector());

            // Validar que todos los asientos estén LIBRES
            boolean todosLibres = asientos.stream().allMatch(a -> a.getEstado() == EstadoAsiento.LIBRE);
            if (!todosLibres || asientos.size() != request.getCantidad()) {
                throw new IllegalStateException("Uno o más asientos seleccionados ya no se encuentran disponibles");
            }

            for (Asiento asiento : asientos) {
                asiento.setEstado(EstadoAsiento.RESERVADO);

                DetalleReserva detalle = new DetalleReserva();
                detalle.setReserva(reserva);
                detalle.setAsiento(asiento);
                reserva.getDetalles().add(detalle);

                asientosReservadosDTO.add(new DetalleAsientoResponse(asiento.getIdAsiento(), asiento.getFila(), asiento.getNumero()));
            }
        } else {
            // Sector no numerado: registrar detalles sin asiento físico asignado
            for (int i = 0; i < request.getCantidad(); i++) {
                DetalleReserva detalle = new DetalleReserva();
                detalle.setReserva(reserva);
                detalle.setAsiento(null);
                reserva.getDetalles().add(detalle);
            }
        }

        // Descontar la disponibilidad del inventario atómicamente
        inventario.setDisponibles(inventario.getDisponibles() - request.getCantidad());
        inventarioSectorRepository.save(inventario);

        Reserva guardada = reservaRepository.save(reserva);

        return mapToReservaResponse(guardada, asientosReservadosDTO);
    }

    // 2. GET /reservas/{id}
    @Transactional(readOnly = true)
    public ReservaResponse obtenerReservaPorId(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new example.microservicio_inventario_reservas.exceptions.RecursoNoEncontradoException("Reserva no encontrada"));
        return mapToReservaResponse(reserva, extraerAsientosDTO(reserva));
    }

    // 3. GET /reservas?clienteId={clienteId}
    @Transactional(readOnly = true)
    public List<ReservaResponse> obtenerReservasPorCliente(Long idCliente) {
        return reservaRepository.findByIdCliente(idCliente).stream()
                .map(r -> mapToReservaResponse(r, extraerAsientosDTO(r)))
                .collect(Collectors.toList());
    }

    // 4. POST /reservas/{id}/confirmar
    @Transactional
    public MensajeResponse confirmarReserva(Long idReserva) {
        Reserva reserva = reservaRepository.buscarConBloqueo(idReserva)
                .orElseThrow(() -> new example.microservicio_inventario_reservas.exceptions.RecursoNoEncontradoException("Reserva no encontrada"));

        InventarioSector inventario = inventarioSectorRepository
                .findByIdEventoAndIdSectorWithLock(reserva.getIdEvento(), reserva.getIdSector())
                .orElseThrow(() -> new example.microservicio_inventario_reservas.exceptions.RecursoNoEncontradoException("Inventario no encontrado"));
        if (inventario.getEstado() != EstadoInventario.HABILITADO)
            throw new IllegalStateException("El inventario se encuentra cancelado");
        if (reserva.getEstado() == EstadoReserva.CONFIRMADA)
            return new MensajeResponse("Reserva confirmada exitosamente", true);
        if (!reserva.getFechaExpiracion().isAfter(LocalDateTime.now(java.time.ZoneOffset.UTC)))
            throw new IllegalStateException("La reserva se encuentra vencida");

        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden confirmar reservas en estado PENDIENTE");
        }

        reserva.setEstado(EstadoReserva.CONFIRMADA);

        // Si es numerado, los asientos pasan a OCUPADO definitivamente
        for (DetalleReserva d : reserva.getDetalles()) {
            if (d.getAsiento() != null) {
                d.getAsiento().setEstado(EstadoAsiento.OCUPADO);
            }
        }

        reservaRepository.save(reserva);
        return new MensajeResponse("Reserva confirmada exitosamente", true);
    }

    // 5. POST /reservas/{id}/liberar
    @Transactional
    public MensajeResponse liberarReserva(Long idReserva) {
        Reserva reserva = reservaRepository.buscarConBloqueo(idReserva)
                .orElseThrow(() -> new example.microservicio_inventario_reservas.exceptions.RecursoNoEncontradoException("Reserva no encontrada"));

        if (reserva.getEstado() == EstadoReserva.LIBERADA || reserva.getEstado() == EstadoReserva.EXPIRADA) {
            if(reserva.getEstado()==EstadoReserva.EXPIRADA)reserva.setEstado(EstadoReserva.LIBERADA);
            return new MensajeResponse("La reserva ya se encuentra liberada o expirada", true);
        }

        if (reserva.getEstado() != EstadoReserva.PENDIENTE)
            throw new IllegalStateException("Una reserva confirmada requiere compensacion o devolucion");
        liberarLogicaReserva(reserva, EstadoReserva.LIBERADA);
        return new MensajeResponse("Reserva liberada y disponibilidad restablecida", true);
    }

    // 6. Proceso programado TTL (Expiración automática de reservas)
    @Transactional
    public MensajeResponse compensarReserva(Long id) {
        Reserva reserva = reservaRepository.buscarConBloqueo(id)
                .orElseThrow(() -> new example.microservicio_inventario_reservas.exceptions.RecursoNoEncontradoException("Reserva no encontrada"));
        if (reserva.getEstado() == EstadoReserva.EXPIRADA) {
            reserva.setEstado(EstadoReserva.LIBERADA);
            return new MensajeResponse("Reserva vencida compensada sin reponer stock nuevamente",true);
        }
        if (reserva.getEstado() == EstadoReserva.LIBERADA)
            return new MensajeResponse("Reserva ya liberada", true);
        if (!devoluciones.findByReservaIdReserva(id).isEmpty())
            throw new IllegalStateException("La reserva tiene devoluciones; no admite compensacion total");
        liberarLogicaReserva(reserva, EstadoReserva.LIBERADA);
        return new MensajeResponse("Reserva compensada", true);
    }

    @Scheduled(fixedDelayString = "${reservas.expiration-interval-ms:30000}")
    @Transactional
    public void expirarReservasVencidas() {
        List<Long> vencidas = reservaRepository.buscarIdsVencidos(LocalDateTime.now(java.time.ZoneOffset.UTC));
        for (Long id : vencidas) {
            Reserva actual = reservaRepository.buscarConBloqueo(id).orElseThrow();
            if (actual.getEstado() == EstadoReserva.PENDIENTE
                    && !actual.getFechaExpiracion().isAfter(LocalDateTime.now(java.time.ZoneOffset.UTC)))
                liberarLogicaReserva(actual, EstadoReserva.EXPIRADA);
        }
    }

    // Helper interno para restaurar disponibilidad de stock y cambiar estados
    private void liberarLogicaReserva(Reserva reserva, EstadoReserva nuevoEstado) {
        InventarioSector inventario = inventarioSectorRepository
                .findByIdEventoAndIdSectorWithLock(reserva.getIdEvento(), reserva.getIdSector())
                .orElseThrow(() -> new example.microservicio_inventario_reservas.exceptions.RecursoNoEncontradoException("Inventario no encontrado"));
        reserva.setEstado(nuevoEstado);

        List<Long> idsAsientos = new ArrayList<>();
        for (DetalleReserva d : reserva.getDetalles()) {
            if (d.getAsiento() != null) {
                idsAsientos.add(d.getAsiento().getIdAsiento());
            }
        }

        if (!idsAsientos.isEmpty() && inventario.getEstado() != EstadoInventario.CANCELADO) {
            reserva.getDetalles().forEach(d -> {
                if (d.getAsiento() != null) d.getAsiento().setEstado(EstadoAsiento.LIBRE);
            });
        }

        if (inventario.getEstado() != EstadoInventario.CANCELADO) {
            inventario.setDisponibles(inventario.getDisponibles() + reserva.getCantidad());
            inventarioSectorRepository.save(inventario);
        }
        reservaRepository.save(reserva);
    }

    private List<DetalleAsientoResponse> extraerAsientosDTO(Reserva reserva) {
        return reserva.getDetalles().stream()
                .filter(d -> d.getAsiento() != null)
                .map(d -> new DetalleAsientoResponse(d.getAsiento().getIdAsiento(), d.getAsiento().getFila(), d.getAsiento().getNumero()))
                .collect(Collectors.toList());
    }

    private ReservaResponse mapToReservaResponse(Reserva r, List<DetalleAsientoResponse> asientos) {
        return new ReservaResponse(
                r.getIdReserva(),
                r.getIdCliente(),
                r.getIdEvento(),
                r.getIdSector(),
                r.getCantidad(),
                r.getFechaCreacion(),
                r.getFechaExpiracion(),
                r.getEstado(),
                asientos
        );
    }
}
