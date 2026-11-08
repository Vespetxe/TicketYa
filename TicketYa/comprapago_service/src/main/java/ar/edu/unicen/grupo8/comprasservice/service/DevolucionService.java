package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.client.*;
import ar.edu.unicen.grupo8.comprasservice.dto.*;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoDevolucion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.time.*;
import java.util.*;

/** Casos de uso internos 6.1–6.4. Los endpoints y su autorización se incorporan en 6.9. */
@Service
@Validated
@Transactional(propagation = Propagation.NEVER)
public class DevolucionService {
    private final CompraService compras;
    private final EntradaClient entradas;
    private final EventoClient eventos;
    private final DevolucionRegistroService registro;
    private final Clock clock;

    public DevolucionService(CompraService compras, EntradaClient entradas, EventoClient eventos,
            DevolucionRegistroService registro, Clock clock) {
        this.compras = compras; this.entradas = entradas; this.eventos = eventos; this.registro = registro; this.clock = clock;
    }

    public DevolucionDTO solicitar(@NotNull @Valid SolicitarDevolucionDTO dto) {
        LocalDateTime solicitud = LocalDateTime.now(clock);
        if (new HashSet<>(dto.entradasIds()).size() != dto.entradasIds().size())
            throw new IllegalArgumentException("La solicitud contiene entradas repetidas");
        var compra = compras.obtenerCompraPorId(dto.compraId());
        DevolucionRegistroService.validarCompra(compra, dto.clienteId());
        var emitidas = entradas.consultarEntradas(dto.compraId());
        var inicio = eventos.obtenerFechaInicio(compra.getEventoId());
        return registro.registrar(dto, emitidas, inicio, solicitud);
    }

    public DevolucionDTO obtener(@NotNull @Positive Long id) { return registro.obtener(id); }

    public List<DevolucionDTO> obtenerPorEstado(@NotNull EstadoDevolucion estado) { return registro.obtenerPorEstado(estado); }

    public DevolucionDTO aprobar(@NotNull @Positive Long id, @NotNull @Valid AprobarDevolucionDTO dto) {
        return registro.decidir(id, EstadoDevolucion.APROBADA, dto.motivo());
    }

    public DevolucionDTO rechazar(@NotNull @Positive Long id, @NotNull @Valid RechazarDevolucionDTO dto) {
        return registro.decidir(id, EstadoDevolucion.RECHAZADA, dto.motivo());
    }
}
