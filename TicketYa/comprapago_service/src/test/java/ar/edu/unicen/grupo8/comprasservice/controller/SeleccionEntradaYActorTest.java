package ar.edu.unicen.grupo8.comprasservice.controller;

import ar.edu.unicen.grupo8.comprasservice.client.EntradaClient;
import ar.edu.unicen.grupo8.comprasservice.dto.EntradaDTO;
import ar.edu.unicen.grupo8.comprasservice.service.*;
import ar.edu.unicen.grupo8.comprasservice.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeleccionEntradaYActorTest {
    @Test void exponeCorrespondenciaPorUnidadSinDependerDelOrdenDeIdentificadores() {
        var compras=mock(CompraService.class);var entradas=mock(EntradaClient.class);var identidad=mock(EntradaIdentidadService.class);
        when(entradas.consultarEntradas(1L)).thenReturn(List.of(
            new EntradaDTO(202L,1L,3L,4L,5L,20L,"EMITIDA","qr-b"),
            new EntradaDTO(101L,1L,3L,4L,5L,10L,"EMITIDA","qr-a")));
        when(identidad.uuid(202L)).thenReturn("uuid-b");when(identidad.uuid(101L)).thenReturn("uuid-a");
        var result=new CompraEntradasController(compras,entradas,identidad).consultar(1L);
        var selected=result.stream().filter(e->e.asientoId().equals(10L)).findFirst().orElseThrow();
        assertEquals(101L,selected.entradaId());assertEquals("uuid-a",selected.ticketId());
        assertEquals(202L,result.get(0).entradaId());assertEquals("uuid-b",result.get(0).ticketId());
    }
    @Test void administrativoExigeActorPositivoAntesDeEjecutarNegocio() throws Exception {
        var devoluciones=mock(DevolucionService.class);var reintegros=mock(ReintegroService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new DevolucionAdminController(devoluciones,reintegros))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/admin/devoluciones?estado=PENDIENTE")).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/devoluciones?estado=PENDIENTE").header("X-Admin-Id","0")).andExpect(status().isBadRequest());
        verifyNoInteractions(devoluciones,reintegros);
        mvc.perform(get("/admin/devoluciones?estado=PENDIENTE").header("X-Admin-Id","7")).andExpect(status().isOk());
        verify(devoluciones).obtenerPorEstado(ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoDevolucion.PENDIENTE);
    }
}
