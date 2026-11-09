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
    @Transactional public void publicar(Long id){control.publicar(id);inventarioSectorRepository.habilitar(id);}
    @Transactional public void habilitar(Long id){control.habilitar(id);if(inventarioSectorRepository.cupos(id)<1)throw new IllegalStateException("Sin disponibilidad para reabrir");inventarioSectorRepository.habilitar(id);}
    @Transactional
    public void suspender(Long id) {control.suspender(id);inventarioSectorRepository.suspender(id);}
    @Autowired private ControlEventoService control;

    @Autowired
    private InventarioSectorRepository inventarioSectorRepository;
    @Autowired
    private AsientoRepository asientoRepository;
    @Autowired private DevolucionInventarioService devoluciones;
    @Autowired private example.microservicio_inventario_reservas.repositories.ReservaRepository reservas;

    // 1. POST /eventos/{idEvento}/inventario
    @Transactional
    public MensajeResponse inicializarInventario(Long idEvento, InicializarInventarioRequest request) {
        if (idEvento == null || idEvento <= 0 || request == null || request.getIdSector() == null
                || request.getIdSector() <= 0 || request.getCapacidadTotal() == null || request.getCapacidadTotal() < 1) {
            throw new IllegalArgumentException("La capacidad total para el evento y sector es invalida");
        }
        control.inicializar(idEvento);
        var existente = inventarioSectorRepository.findByIdEventoAndIdSector(idEvento, request.getIdSector());
        if (existente.isPresent()) {
            InventarioSector anterior = existente.get();
            boolean coincide = anterior.getCapacidadTotal() != null
                    && anterior.getCapacidadTotal().equals(request.getCapacidadTotal())
                    && anterior.isEsNumerado() == request.isEsNumerado();
            if (coincide && request.isEsNumerado()) {
                coincide = request.getAsientos() != null && request.getAsientos().size() == anterior.getAsientos().size()
                        && request.getAsientos().stream().allMatch(dto -> anterior.getAsientos().stream().anyMatch(a ->
                        java.util.Objects.equals(a.getCatalogoAsientoId(),dto.getCatalogoAsientoId())
                                && java.util.Objects.equals(a.getFila(),dto.getFila()) && java.util.Objects.equals(a.getNumero(),dto.getNumero())));
            }
            if (coincide) return new MensajeResponse("Inventario inicializado exitosamente",true);
            throw new IllegalArgumentException("El inventario para este evento y sector ya existe");
        }
        if (request.isEsNumerado() && (request.getAsientos() == null || request.getAsientos().size() != request.getCapacidadTotal()))
            throw new IllegalArgumentException("La grilla debe coincidir con la capacidad");
        if (request.getAsientos() != null) {
            java.util.Set<String> posiciones = new java.util.HashSet<>();
            java.util.Set<Long> referencias = new java.util.HashSet<>();
            for (AsientoDTO a : request.getAsientos()) {
                if (a == null || a.getFila() == null || a.getFila().isBlank() || a.getNumero() == null || a.getNumero() <= 0
                        || !posiciones.add(a.getFila()+":"+a.getNumero())
                        || (a.getCatalogoAsientoId()!=null && (a.getCatalogoAsientoId()<=0 || !referencias.add(a.getCatalogoAsientoId()))))
                    throw new IllegalArgumentException("Grilla invalida o duplicada");
            }
        }

        InventarioSector inventario = new InventarioSector();
        inventario.setIdEvento(idEvento);
        inventario.setIdSector(request.getIdSector());
        inventario.setEsNumerado(request.isEsNumerado());
        inventario.setCapacidadTotal(request.getCapacidadTotal());
        inventario.setDisponibles(request.getCapacidadTotal());
        inventario.setEstado(EstadoInventario.PREPARADO);

        if (request.isEsNumerado() && request.getAsientos() != null) {
            for (AsientoDTO aDto : request.getAsientos()) {
                Asiento asiento = new Asiento();
                asiento.setCatalogoAsientoId(aDto.getCatalogoAsientoId());
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
                .orElseThrow(() -> new example.microservicio_inventario_reservas.exceptions.RecursoNoEncontradoException("Inventario no encontrado"));

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
        if(request==null || request.getIdReserva()==null || request.getEntradasIds()==null
                || request.getCantidad()==null || request.getCantidad()<=0 || request.getEntradasIds().size()!=request.getCantidad()
                || (request.getIdsAsientos()!=null && request.getIdsAsientos().size()!=request.getCantidad()))
            throw new IllegalArgumentException("Se requiere reserva y una identidad estable por entrada");
        var reserva=reservas.buscarConBloqueo(request.getIdReserva()).orElseThrow(() ->
                new example.microservicio_inventario_reservas.exceptions.RecursoNoEncontradoException("Reserva no encontrada"));
        if(!java.util.Objects.equals(reserva.getIdEvento(),request.getIdEvento()) || !java.util.Objects.equals(reserva.getIdSector(),request.getIdSector()))
            throw new IllegalArgumentException("La reserva no corresponde al evento y sector");
        java.util.List<DevolverLugaresRequest.Unidad> unidades=new java.util.ArrayList<>();
        for(int n=0;n<request.getCantidad();n++) unidades.add(new DevolverLugaresRequest.Unidad(request.getEntradasIds().get(n),
                request.getIdsAsientos()==null ? null : request.getIdsAsientos().get(n)));
        devoluciones.devolver(request.getIdReserva(),new DevolverLugaresRequest(unidades));
        return new MensajeResponse("Lugares reintegrados al inventario exitosamente", true);
    }

    // 4. POST /eventos/{idEvento}/cancelar-inventario
    @Transactional
    public MensajeResponse cancelarInventarioEvento(Long idEvento) {
        control.cancelar(idEvento);
        inventarioSectorRepository.cancelarInventarioPorEvento(idEvento);
        asientoRepository.cancelarAsientosPorEvento(idEvento);
        return new MensajeResponse("Inventario del evento invalidado correctamente por cancelacion", true);
    }
}
