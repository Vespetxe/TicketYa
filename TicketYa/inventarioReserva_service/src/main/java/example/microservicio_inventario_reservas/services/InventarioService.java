package example.microservicio_inventario_reservas.services;

import example.microservicio_inventario_reservas.dtos.*;
import example.microservicio_inventario_reservas.entities.Asiento;
import example.microservicio_inventario_reservas.entities.InventarioSector;
import example.microservicio_inventario_reservas.enums.EstadoAsiento;
import example.microservicio_inventario_reservas.enums.EstadoInventario;
import example.microservicio_inventario_reservas.repositories.AsientoRepository;
import example.microservicio_inventario_reservas.repositories.InventarioSectorRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("InventarioService")
public class InventarioService {

    @Autowired
    private InventarioSectorRepository inventarioSectorRepository;
    @Autowired
    private AsientoRepository asientoRepository;

    // 1. POST /eventos/{idEvento}/inventario
    @Transactional
    public MensajeResponse inicializarInventario(Long idEvento, InicializarInventarioRequest request) {
        if (request.getCapacidadTotal() < 1) {
            throw new IllegalArgumentException("La capacidad total para el evento y sector es invalida");
        }
        if (inventarioSectorRepository.findByIdEventoAndIdSector(idEvento, request.getIdSector()).isPresent()) {
            throw new IllegalArgumentException("El inventario para este evento y sector ya existe");
        }

        InventarioSector inventario = new InventarioSector();
        inventario.setIdEvento(idEvento);
        inventario.setIdSector(request.getIdSector());
        inventario.setEsNumerado(request.isEsNumerado());
        inventario.setCapacidadTotal(request.getCapacidadTotal());
        inventario.setDisponibles(request.getCapacidadTotal());
        inventario.setEstado(EstadoInventario.HABILITADO);

        if (request.isEsNumerado() && request.getAsientos() != null) {
            for (AsientoDTO aDto : request.getAsientos()) {
                Asiento asiento = new Asiento();
                asiento.setFila(aDto.getFila());
                asiento.setNumero(aDto.getNumero());
                asiento.setEstado(EstadoAsiento.LIBRE);
                asiento.setInventarioSector(inventario);
                inventario.getAsientos().add(asiento);
            }
        }

        inventarioSectorRepository.save(inventario);
        return new MensajeResponse("Inventario inicializado exitosamente", true);
    }

    // 2. GET /disponibilidad/eventos/{idEvento}/sectores/{idSector}
    @Transactional(readOnly = true)
    public DisponibilidadResponse consultarDisponibilidad(Long idEvento, Long idSector) {
        InventarioSector inventario = inventarioSectorRepository.findByIdEventoAndIdSector(idEvento, idSector)
                .orElseThrow(() -> new RuntimeException("Inventario no encontrado"));

        List<DetalleAsientoResponse> asientosLibres = null;
        if (inventario.isEsNumerado()) {
            asientosLibres = inventario.getAsientos().stream()
                    .filter(a -> a.getEstado() == EstadoAsiento.LIBRE)
                    .map(a -> new DetalleAsientoResponse(a.getIdAsiento(), a.getFila(), a.getNumero()))
                    .collect(Collectors.toList());
        }

        return new DisponibilidadResponse(
                inventario.getIdEvento(),
                inventario.getIdSector(),
                inventario.isEsNumerado(),
                inventario.getCapacidadTotal(),
                inventario.getDisponibles(),
                asientosLibres
        );
    }

    // 3. POST /ocupaciones/liberar (Devolución)
    @Transactional
    public MensajeResponse liberarPorDevolucion(LiberarDevolucionRequest request) {
        InventarioSector inventario = inventarioSectorRepository.findByIdEventoAndIdSector(request.getIdEvento(), request.getIdSector())
                .orElseThrow(() -> new RuntimeException("Inventario no encontrado"));
        if (inventario.isEsNumerado()) {
            if (request.getIdsAsientos() != null && !request.getIdsAsientos().isEmpty()) {
                for (Long id : request.getIdsAsientos()) {
                    Asiento a = asientoRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Asiento no encontrado: " + id));
                }
                asientoRepository.actualizarEstadoAsientos(request.getIdsAsientos(), EstadoAsiento.LIBRE);
            }
        }

        inventarioSectorRepository.incrementarDisponibilidad(request.getIdEvento(), request.getIdSector(), request.getCantidad());
        return new MensajeResponse("Lugares reintegrados al inventario exitosamente", true);
    }

    // 4. POST /eventos/{idEvento}/cancelar-inventario
    @Transactional
    public MensajeResponse cancelarInventarioEvento(Long idEvento) {
        inventarioSectorRepository.cancelarInventarioPorEvento(idEvento);
        asientoRepository.cancelarAsientosPorEvento(idEvento);
        return new MensajeResponse("Inventario del evento invalidado correctamente por cancelacion", true);
    }
}
