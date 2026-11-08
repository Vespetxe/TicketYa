
package ar.edu.unicen.grupo8.comprasservice.dto;

import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CrearCompraDTOTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void configurarValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    private CrearCompraDTO crearCompraValida() {
        DetalleCompraDTO detalle = new DetalleCompraDTO();
        detalle.setSectorId(1L);
        detalle.setCantidad(2);

        CrearCompraDTO compra = new CrearCompraDTO();
        compra.setClienteId(10L);
        compra.setEventoId(20L);
        compra.setReservaId(30L);
        compra.setDetalles(List.of(detalle));

        return compra;
    }

    @Test
    void compraValidaNoDebeTenerErrores() {
        CrearCompraDTO compra = crearCompraValida();

        assertTrue(validator.validate(compra).isEmpty());
    }

    @Test
    void debeRechazarClienteNulo() {
        CrearCompraDTO compra = crearCompraValida();
        compra.setClienteId(null);

        assertFalse(validator.validate(compra).isEmpty());
    }

    @Test
    void debeRechazarEventoNegativo() {
        CrearCompraDTO compra = crearCompraValida();
        compra.setEventoId(-1L);

        assertFalse(validator.validate(compra).isEmpty());
    }

    @Test
    void debeRechazarReservaNula() {
        CrearCompraDTO compra = crearCompraValida();
        compra.setReservaId(null);

        assertFalse(validator.validate(compra).isEmpty());
    }

    @Test
    void debeRechazarListaDeDetallesVacia() {
        CrearCompraDTO compra = crearCompraValida();
        compra.setDetalles(List.of());

        assertFalse(validator.validate(compra).isEmpty());
    }

    @Test
    void debeRechazarCantidadCeroEnUnDetalle() {
        CrearCompraDTO compra = crearCompraValida();
        compra.getDetalles().get(0).setCantidad(0);

        assertFalse(validator.validate(compra).isEmpty());
    }

    @Test
    void debeRechazarSectorNuloEnUnDetalle() {
        CrearCompraDTO compra = crearCompraValida();
        compra.getDetalles().get(0).setSectorId(null);

        assertFalse(validator.validate(compra).isEmpty());
    }


    @Test
    void debeRechazarClienteCero() {
        CrearCompraDTO compra = crearCompraValida();
        compra.setClienteId(0L);

        assertFalse(validator.validate(compra).isEmpty());
    }

    @Test
    void debeRechazarDetallesNulos() {
        CrearCompraDTO compra = crearCompraValida();
        compra.setDetalles(null);

        assertFalse(validator.validate(compra).isEmpty());
    }

    @Test
    void debeRechazarElementoNuloEnDetalles() {
        CrearCompraDTO compra = crearCompraValida();
        compra.setDetalles(java.util.Arrays.asList((DetalleCompraDTO) null));
        assertFalse(validator.validate(compra).isEmpty());
    }

    @Test
    void debeRechazarAsientoNegativo() {
        CrearCompraDTO compra = crearCompraValida();
        compra.getDetalles().get(0).setAsientoId(-1L);
        assertFalse(validator.validate(compra).isEmpty());
    }

    @Test
    void debeRechazarCantidadNegativa() {
        CrearCompraDTO compra = crearCompraValida();
        compra.getDetalles().get(0).setCantidad(-2);

        assertFalse(validator.validate(compra).isEmpty());
    }

    @Test
    void debeRechazarSectorNegativo() {
        CrearCompraDTO compra = crearCompraValida();
        compra.getDetalles().get(0).setSectorId(-5L);

        assertFalse(validator.validate(compra).isEmpty());
    }


    @Test
    void debeIgnorarPrecioEnviadoPorCliente() throws Exception {

        String json = """
            {
                "clienteId": 10,
                "eventoId": 20,
                "reservaId": 30,
                "detalles": [
                    {
                        "sectorId": 1,
                        "cantidad": 2,
                        "precioUnitario": 1.00
                    }
                ]
            }
            """;

        ObjectMapper mapper = new ObjectMapper();

        CrearCompraDTO dto = mapper.readValue(
                json,
                CrearCompraDTO.class
        );

        assertEquals(1, dto.getDetalles().size());
        assertEquals(1L, dto.getDetalles().get(0).getSectorId());
        assertEquals(2, dto.getDetalles().get(0).getCantidad());
    }


}
