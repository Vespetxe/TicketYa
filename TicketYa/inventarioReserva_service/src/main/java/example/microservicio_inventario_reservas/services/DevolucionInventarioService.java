package example.microservicio_inventario_reservas.services;

import example.microservicio_inventario_reservas.dtos.DevolverLugaresRequest;
import example.microservicio_inventario_reservas.entities.*;
import example.microservicio_inventario_reservas.enums.*;
import example.microservicio_inventario_reservas.repositories.*;
import example.microservicio_inventario_reservas.exceptions.RecursoNoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class DevolucionInventarioService {
    private final ReservaRepository reservas;
    private final InventarioSectorRepository inventarios;
    private final LugarDevueltoRepository devueltos;
    public DevolucionInventarioService(ReservaRepository r, InventarioSectorRepository i, LugarDevueltoRepository d) {
        reservas=r; inventarios=i; devueltos=d;
    }
    @Transactional
    public List<String> devolver(Long id, DevolverLugaresRequest request) {
        Reserva r=reservas.buscarConBloqueo(id).orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
        if(r.getEstado()!=EstadoReserva.CONFIRMADA) throw new IllegalStateException("La reserva debe estar confirmada");
        InventarioSector i=inventarios.findByIdEventoAndIdSectorWithLock(r.getIdEvento(),r.getIdSector()).orElseThrow();
        if(request==null || request.entradas()==null || request.entradas().isEmpty()) throw new IllegalArgumentException("Seleccion vacia");
        Set<String> ids=new HashSet<>();
        List<LugarDevuelto> existentes=devueltos.findByReservaIdReserva(id);
        Set<Long> asientosDevueltos=new HashSet<>();
        existentes.forEach(d -> {if(d.getAsientoId()!=null) asientosDevueltos.add(d.getAsientoId());});
        int cantidad=existentes.size();
        for(var unidad:request.entradas()) {
            if(unidad==null || unidad.entradaId()==null || unidad.entradaId().isBlank() || unidad.entradaId().length()>64 || !ids.add(unidad.entradaId()))
                throw new IllegalArgumentException("Identidad de entrada invalida o repetida");
            var previo=devueltos.findByEntradaId(unidad.entradaId());
            if(previo.isPresent()) {
                if(!id.equals(previo.get().getReserva().getIdReserva()) || !Objects.equals(unidad.asientoId(),previo.get().getAsientoId()))
                    throw new IllegalStateException("Entrada ya asociada a otra seleccion");
                continue;
            }
            if(++cantidad>r.getCantidad()) throw new IllegalStateException("La devolucion supera la reserva");
            Asiento asiento=null;
            if(i.isEsNumerado()) {
                asiento=r.getDetalles().stream().map(DetalleReserva::getAsiento).filter(Objects::nonNull)
                        .filter(a -> a.getIdAsiento().equals(unidad.asientoId())).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Asiento ajeno a la reserva"));
                if(!asientosDevueltos.add(asiento.getIdAsiento())) throw new IllegalStateException("Asiento ya devuelto");
                if(i.getEstado()!=EstadoInventario.CANCELADO && asiento.getEstado()!=EstadoAsiento.OCUPADO)
                    throw new IllegalStateException("Asiento no ocupado por la reserva");
            } else if(unidad.asientoId()!=null) throw new IllegalArgumentException("El sector no es numerado");
            LugarDevuelto d=new LugarDevuelto(); d.setEntradaId(unidad.entradaId()); d.setReserva(r); d.setAsientoId(unidad.asientoId());
            devueltos.save(d);
            if(i.getEstado()!=EstadoInventario.CANCELADO) {
                if(asiento!=null) asiento.setEstado(EstadoAsiento.LIBRE);
                i.setDisponibles(i.getDisponibles()+1);
                if(i.getDisponibles()>i.getCapacidadTotal()) throw new IllegalStateException("Stock excedido");
            }
        }
        return devueltos.findByReservaIdReserva(id).stream().map(LugarDevuelto::getEntradaId).sorted().toList();
    }
    @Transactional(readOnly=true)
    public List<String> consultar(Long id) {
        if(!reservas.existsById(id)) throw new RecursoNoEncontradoException("Reserva no encontrada");
        return devueltos.findByReservaIdReserva(id).stream().map(LugarDevuelto::getEntradaId).sorted().toList();
    }
}
