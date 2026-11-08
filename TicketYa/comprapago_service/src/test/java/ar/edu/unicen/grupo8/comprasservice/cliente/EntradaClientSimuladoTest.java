
package ar.edu.unicen.grupo8.comprasservice.client;

import ar.edu.unicen.grupo8.comprasservice.dto.DetalleEmisionDTO;
import ar.edu.unicen.grupo8.comprasservice.dto.EntradaDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntradaClientSimuladoTest {

    @Test
    void anulacionSinEntradasImpideUnaEmisionDemorada() {
        entradaClient.anularEntradas(100L);
        assertTrue(entradaClient.consultarEntradas(100L).isEmpty());
        assertThrows(IllegalStateException.class, () -> emitir(100L));
        entradaClient.anularEntradas(100L);
        assertTrue(entradaClient.consultarEntradas(100L).isEmpty());
    }

    private final EntradaClientSimulado entradaClient =
            new EntradaClientSimulado();

    private static final Long EVENTO_ID = 20L;
    private static final Long CLIENTE_ID = 10L;

    private List<DetalleEmisionDTO> unDetalle() {
        return List.of(
                new DetalleEmisionDTO(30L, 40L, 1)
        );
    }

    private List<EntradaDTO> emitir(Long compraId) {
        return entradaClient.solicitarEntradas(
                compraId,
                EVENTO_ID,
                CLIENTE_ID,
                unDetalle()
        );
    }

    @Test
    void debeSolicitarEntradasCorrectamente() {

        Long compraId = 1L;

        List<EntradaDTO> entradas = emitir(compraId);

        assertNotNull(entradas);
        assertEquals(1, entradas.size());

        EntradaDTO entrada = entradas.get(0);

        assertEquals(compraId, entrada.compraId());
        assertEquals(EVENTO_ID, entrada.eventoId());
        assertEquals(CLIENTE_ID, entrada.clienteId());
        assertEquals(30L, entrada.sectorId());
        assertEquals(40L, entrada.asientoId());
        assertEquals("EMITIDA", entrada.estado());
        assertNotNull(entrada.entradaId());
        assertNotNull(entrada.codigo());
    }

    @Test
    void debeConsultarEntradasEmitidas() {

        Long compraId = 2L;

        List<EntradaDTO> emitidas = emitir(compraId);

        List<EntradaDTO> consultadas =
                entradaClient.consultarEntradas(compraId);

        assertEquals(1, consultadas.size());
        assertEquals(emitidas, consultadas);
        assertEquals("EMITIDA", consultadas.get(0).estado());
    }

    @Test
    void debeDevolverListaVaciaSiNoHayEntradas() {

        List<EntradaDTO> entradas =
                entradaClient.consultarEntradas(999L);

        assertNotNull(entradas);
        assertTrue(entradas.isEmpty());
    }

    @Test
    void debeAnularEntradasCorrectamente() {

        Long compraId = 3L;

        List<EntradaDTO> emitidas = emitir(compraId);

        EntradaDTO entradaOriginal = emitidas.get(0);

        entradaClient.anularEntradas(compraId);

        List<EntradaDTO> anuladas =
                entradaClient.consultarEntradas(compraId);

        assertEquals(1, anuladas.size());

        EntradaDTO entradaAnulada = anuladas.get(0);

        assertEquals("ANULADA", entradaAnulada.estado());
        assertEquals(
                entradaOriginal.entradaId(),
                entradaAnulada.entradaId()
        );
        assertEquals(
                entradaOriginal.codigo(),
                entradaAnulada.codigo()
        );
        assertEquals(compraId, entradaAnulada.compraId());
        assertEquals(EVENTO_ID, entradaAnulada.eventoId());
        assertEquals(CLIENTE_ID, entradaAnulada.clienteId());
        assertEquals(30L, entradaAnulada.sectorId());
        assertEquals(40L, entradaAnulada.asientoId());
    }

    @Test
    void noDebeDuplicarEntradasAlSolicitarlasDosVeces() {

        Long compraId = 4L;

        List<EntradaDTO> primeraSolicitud = emitir(compraId);
        List<EntradaDTO> segundaSolicitud = emitir(compraId);

        assertEquals(1, primeraSolicitud.size());
        assertEquals(1, segundaSolicitud.size());

        assertEquals(primeraSolicitud, segundaSolicitud);

        assertEquals(
                1,
                entradaClient.consultarEntradas(compraId).size()
        );
    }

    @Test
    void noDebeModificarEntradasAlAnularlasDosVeces() {

        Long compraId = 5L;

        emitir(compraId);

        entradaClient.anularEntradas(compraId);

        List<EntradaDTO> primeraAnulacion =
                entradaClient.consultarEntradas(compraId);

        entradaClient.anularEntradas(compraId);

        List<EntradaDTO> segundaAnulacion =
                entradaClient.consultarEntradas(compraId);

        assertEquals(1, primeraAnulacion.size());
        assertEquals(1, segundaAnulacion.size());

        assertEquals("ANULADA", primeraAnulacion.get(0).estado());
        assertEquals("ANULADA", segundaAnulacion.get(0).estado());

        assertEquals(primeraAnulacion, segundaAnulacion);
    }

    @Test
    void debeRechazarIdentificadoresDeCompraInvalidos() {

        assertThrows(
                IllegalArgumentException.class,
                () -> emitir(null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> emitir(0L)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> entradaClient.consultarEntradas(-1L)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> entradaClient.anularEntradas(null)
        );
    }

    @Test
    void noDebeReemitirEntradasAnuladas() {

        Long compraId = 6L;

        emitir(compraId);

        entradaClient.anularEntradas(compraId);

        assertThrows(
                IllegalStateException.class,
                () -> emitir(compraId)
        );

        List<EntradaDTO> entradas =
                entradaClient.consultarEntradas(compraId);

        assertEquals(1, entradas.size());
        assertEquals("ANULADA", entradas.get(0).estado());
    }

    // TASK 5.5 - EMISION DE MULTIPLES ENTRADAS

    @Test
    void debeEmitirCincoEntradasParaUnaCompra() {

        Long compraId = 7L;

        List<DetalleEmisionDTO> detalles = List.of(
                new DetalleEmisionDTO(30L, null, 3),
                new DetalleEmisionDTO(40L, null, 2)
        );

        List<EntradaDTO> entradas =
                entradaClient.solicitarEntradas(
                        compraId,
                        EVENTO_ID,
                        CLIENTE_ID,
                        detalles
                );

        assertEquals(5, entradas.size());

        assertEquals(
                3,
                entradas.stream()
                        .filter(e -> e.sectorId().equals(30L))
                        .count()
        );

        assertEquals(
                2,
                entradas.stream()
                        .filter(e -> e.sectorId().equals(40L))
                        .count()
        );

        assertTrue(
                entradas.stream()
                        .allMatch(e -> "EMITIDA".equals(e.estado()))
        );

        assertEquals(
                5,
                entradas.stream()
                        .map(EntradaDTO::entradaId)
                        .distinct()
                        .count()
        );

        assertEquals(
                5,
                entradas.stream()
                        .map(EntradaDTO::codigo)
                        .distinct()
                        .count()
        );

        assertEquals(
                entradas,
                entradaClient.consultarEntradas(compraId)
        );
    }

    @Test
    void noDebeDuplicarMultiplesEntradasAlReintentar() {

        Long compraId = 8L;

        List<DetalleEmisionDTO> detalles = List.of(
                new DetalleEmisionDTO(30L, null, 3),
                new DetalleEmisionDTO(40L, null, 2)
        );

        List<EntradaDTO> primeraSolicitud =
                entradaClient.solicitarEntradas(
                        compraId,
                        EVENTO_ID,
                        CLIENTE_ID,
                        detalles
                );

        List<EntradaDTO> segundaSolicitud =
                entradaClient.solicitarEntradas(
                        compraId,
                        EVENTO_ID,
                        CLIENTE_ID,
                        detalles
                );

        assertEquals(5, primeraSolicitud.size());
        assertEquals(5, segundaSolicitud.size());
        assertEquals(primeraSolicitud, segundaSolicitud);

        assertEquals(
                5,
                entradaClient.consultarEntradas(compraId).size()
        );
    }

    @Test
    void debeRechazarDatosInvalidosParaEmitirEntradas() {

        List<DetalleEmisionDTO> detalles = unDetalle();

        assertThrows(
                IllegalArgumentException.class,
                () -> entradaClient.solicitarEntradas(
                        9L, null, CLIENTE_ID, detalles
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> entradaClient.solicitarEntradas(
                        9L, EVENTO_ID, 0L, detalles
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> entradaClient.solicitarEntradas(
                        9L, EVENTO_ID, CLIENTE_ID, List.of()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> entradaClient.solicitarEntradas(
                        9L,
                        EVENTO_ID,
                        CLIENTE_ID,
                        List.of(new DetalleEmisionDTO(30L, null, 0))
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> entradaClient.solicitarEntradas(
                        9L,
                        EVENTO_ID,
                        CLIENTE_ID,
                        List.of(new DetalleEmisionDTO(null, null, 1))
                )
        );
    }
}
