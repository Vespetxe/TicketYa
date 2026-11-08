package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.client.*;
import ar.edu.unicen.grupo8.comprasservice.dto.*;
import ar.edu.unicen.grupo8.comprasservice.entity.*;
import ar.edu.unicen.grupo8.comprasservice.exception.CompraNoEncontradaException;
import ar.edu.unicen.grupo8.comprasservice.repository.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.math.*;
import java.util.List;

@Service
@Validated
public class CompraService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CompraService.class);
    private final CompraRepository compraRepository;
    private final ReservaClient reservaClient;
    private final EventoClient eventoClient;
    private final CompraRegistroService registro;
    private final int limite;

    @Autowired
    public CompraService(CompraRepository compras, ReservaClient reservas, EventoClient eventos,
            CompraRegistroService registro, @Value("${compras.limite-entradas-por-cliente-evento:6}") int limite) {
        if (limite <= 0) throw new IllegalArgumentException("El limite debe ser positivo");
        this.compraRepository = compras;
        this.reservaClient = reservas;
        this.eventoClient = eventos;
        this.registro = registro;
        this.limite = limite;
    }

    public CompraService(CompraRepository compras, ReservaClient reservas, EventoClient eventos,
            ControlLimiteEntradasRepository limites, int limite) {
        this(compras, reservas, eventos, new CompraRegistroService(compras, limites), limite);
    }

    @Transactional(propagation = Propagation.NEVER)
    public Compra crearCompra(@Valid CrearCompraDTO dto) {
        if (!eventoClient.eventoValido(dto.getEventoId())) {
            throw new IllegalArgumentException("El evento indicado no es valido para realizar una compra");
        }
        if (!reservaClient.reservaValida(dto.getReservaId(), dto.getClienteId(), dto.getEventoId())) {
            throw new IllegalArgumentException("La reserva no es valida para el cliente y evento indicados");
        }
        ReservaDTO reserva = reservaClient.consultarReserva(dto.getReservaId());
        if (reserva == null || !dto.getReservaId().equals(reserva.reservaId()))
            throw new IllegalArgumentException("La respuesta no corresponde a la reserva solicitada");
        SeleccionReserva.validar(reserva, dto.getDetalles().stream()
                .map(d -> new DetalleEmisionDTO(d.getSectorId(), d.getAsientoId(), d.getCantidad())).toList());
        Compra compra = new Compra();
        compra.setClienteId(dto.getClienteId());
        compra.setEventoId(dto.getEventoId());
        compra.setReservaId(dto.getReservaId());
        compra.setReservaVencimiento(reserva.fechaVencimiento());
        BigDecimal total = BigDecimal.ZERO;
        for (var d : dto.getDetalles()) {
            BigDecimal precio = eventoClient.obtenerPrecioSector(dto.getEventoId(), d.getSectorId());
            if (precio == null || precio.signum() <= 0) throw new IllegalArgumentException("El precio del sector debe ser mayor a cero");
            try { precio = precio.setScale(2, RoundingMode.UNNECESSARY); }
            catch (ArithmeticException ex) { throw new IllegalArgumentException("El precio debe tener como maximo dos decimales"); }
            DetalleCompra detalle = new DetalleCompra();
            detalle.setSectorId(d.getSectorId());
            detalle.setAsientoId(d.getAsientoId());
            detalle.setCantidad(d.getCantidad());
            detalle.setPrecioUnitario(precio);
            detalle.setSubtotal(precio.multiply(BigDecimal.valueOf(d.getCantidad())));
            total = total.add(detalle.getSubtotal());
            compra.agregarDetalle(detalle);
        }
        if (total.precision() > 12) throw new IllegalArgumentException("El importe supera el maximo admitido");
        compra.setImporteTotal(total);
        return registro.registrar(compra, limite);
    }

    @Transactional(readOnly = true)
    public Compra obtenerCompraPorId(Long id) {
        validarId(id);
        Compra c = compraRepository.findById(id).orElseThrow(() -> new CompraNoEncontradaException(id));
        c.getDetalles().size();
        c.getEntradasIds().size();
        return c;
    }

    @Transactional(readOnly = true)
    public List<Compra> obtenerComprasPorCliente(Long clienteId) {
        validarId(clienteId);
        var compras = compraRepository.findByClienteId(clienteId);
        compras.forEach(c -> { c.getDetalles().size(); c.getEntradasIds().size(); });
        return compras;
    }

    @Transactional(propagation = Propagation.NEVER)
    public Compra cancelarCompra(Long id) {
        validarId(id);
        Compra compra = registro.solicitarCancelacion(id);
        if (compra.getEtapa() == ar.edu.unicen.grupo8.comprasservice.entity.enums.EtapaCompra.FINALIZADA) return compra;
        try {
            ReservaDTO reserva = reservaClient.consultarReserva(compra.getReservaId());
            if (reserva == null) return compra;
            if (!"LIBERADA".equals(reserva.estado())) reservaClient.liberarReserva(compra.getReservaId());
            reserva = reservaClient.consultarReserva(compra.getReservaId());
            if (reserva == null || !"LIBERADA".equals(reserva.estado())) return compra;
        } catch (RuntimeException ex) {
            log.warn("Compra {} cancelada con liberacion pendiente ({})", id, ex.getClass().getSimpleName());
            return compra;
        }
        return registro.finalizarCancelacion(id);
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("El identificador debe ser positivo");
    }
}
