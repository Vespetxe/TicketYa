package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.dto.*;
import java.util.*;
import java.util.stream.Collectors;

public final class SeleccionReserva {
    private SeleccionReserva() { }

    public static Map<String, Long> agrupar(List<DetalleEmisionDTO> detalles) {
        if (detalles == null || detalles.isEmpty()) throw new IllegalArgumentException("La reserva debe identificar sus entradas");
        Set<Long> asientos = new HashSet<>();
        for (var d : detalles) {
            if (d == null || d.sectorId() == null || d.sectorId() <= 0 || d.cantidad() == null || d.cantidad() <= 0
                    || (d.asientoId() != null && (d.asientoId() <= 0 || d.cantidad() != 1 || !asientos.add(d.asientoId())))) {
                throw new IllegalArgumentException("Seleccion de entradas invalida o asiento duplicado");
            }
        }
        return detalles.stream().collect(Collectors.groupingBy(
                d -> d.sectorId() + ":" + d.asientoId(), Collectors.summingLong(DetalleEmisionDTO::cantidad)));
    }

    public static void validar(ReservaDTO reserva, List<DetalleEmisionDTO> detalles) {
        if (reserva == null || !agrupar(detalles).equals(agrupar(reserva.detalles()))) {
            throw new IllegalArgumentException("Los detalles no coinciden con la reserva");
        }
    }
}
