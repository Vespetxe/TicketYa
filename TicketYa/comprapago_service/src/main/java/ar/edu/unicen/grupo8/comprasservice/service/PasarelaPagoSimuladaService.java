package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.client.AlmacenSimulado;
import ar.edu.unicen.grupo8.comprasservice.dto.ResultadoPasarela;
import ar.edu.unicen.grupo8.comprasservice.exception.ComunicacionPasarelaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PasarelaPagoSimuladaService implements PasarelaPagoService {
    public record Operacion(BigDecimal importe, boolean aprobado, String referencia) {
        ResultadoPasarela resultado() { return new ResultadoPasarela(aprobado, referencia); }
    }
    private final AlmacenSimulado almacen;
    public PasarelaPagoSimuladaService() { this(new AlmacenSimulado.Memoria()); }
    @Autowired
    public PasarelaPagoSimuladaService(AlmacenSimulado almacen) { this.almacen = almacen; }

    @Override
    public ResultadoPasarela procesarPago(String clave, BigDecimal importe) {
        validar(clave, importe);
        Operacion resultado = almacen.actualizar("cobro:" + clave, Operacion.class, anterior -> {
            if (anterior != null) { compatible(anterior, importe); return anterior; }
            return new Operacion(importe, importe.compareTo(new BigDecimal("100000")) <= 0, UUID.randomUUID().toString());
        });
        if (clave.startsWith("simular-timeout")) throw new ComunicacionPasarelaException("No se pudo obtener una respuesta de la pasarela");
        return resultado.resultado();
    }
    @Override
    public ResultadoPasarela consultarPago(String clave) {
        validarClave(clave);
        Operacion r = almacen.actualizar("cobro:" + clave, Operacion.class, v -> v);
        return r == null ? null : r.resultado();
    }
    @Override
    public ResultadoPasarela reintegrarPago(String clave, BigDecimal importe) {
        return reintegrarPago(clave, clave, importe);
    }
    @Override
    public ResultadoPasarela consultarReintegro(String clave) { return consultarReintegro(clave, clave); }
    public record Reembolsos(java.util.Map<String, Operacion> operaciones) { }
    @Override
    public ResultadoPasarela reintegrarPago(String clavePago, String clave, BigDecimal importe) {
        validar(clave, importe); validarClave(clavePago);
        if (importe.scale() > 2) throw new IllegalArgumentException("El importe admite hasta dos decimales");
        Operacion pago = almacen.actualizar("cobro:" + clavePago, Operacion.class, v -> v);
        if (pago == null || !pago.aprobado()) throw new IllegalStateException("No existe un pago aprobado para reintegrar");
        almacen.actualizar("reintegro-vinculo:" + clave, String.class, anterior -> {
            if (anterior != null && !anterior.equals(clavePago)) throw new IllegalStateException("Clave usada para otro pago");
            return clavePago;
        });
        // Reconoce compensaciones persistidas por versiones anteriores.
        Operacion legado = almacen.actualizar("reintegro:" + clavePago, Operacion.class, v -> v);
        Reembolsos r = almacen.actualizar("reintegros-pago:" + clavePago, Reembolsos.class, anterior -> {
            var operaciones = new java.util.HashMap<String, Operacion>(anterior == null ? java.util.Map.of() : anterior.operaciones());
            if (legado != null) operaciones.putIfAbsent(clavePago, legado);
            if (operaciones.containsKey(clave)) { compatible(operaciones.get(clave), importe); return new Reembolsos(operaciones); }
            BigDecimal total = operaciones.values().stream().filter(Operacion::aprobado).map(Operacion::importe)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).add(importe);
            if (total.compareTo(pago.importe()) > 0) throw new IllegalArgumentException("El reintegro supera el importe pagado");
            operaciones.put(clave, new Operacion(importe, true, "REINT-" + UUID.randomUUID()));
            return new Reembolsos(java.util.Map.copyOf(operaciones));
        });
        return r.operaciones().get(clave).resultado();
    }
    @Override
    public ResultadoPasarela consultarReintegro(String clavePago, String clave) {
        validarClave(clavePago); validarClave(clave);
        String vinculo = almacen.actualizar("reintegro-vinculo:" + clave, String.class, v -> v);
        if (vinculo != null && !vinculo.equals(clavePago)) throw new IllegalStateException("Clave usada para otro pago");
        Reembolsos r = almacen.actualizar("reintegros-pago:" + clavePago, Reembolsos.class, v -> v);
        Operacion op = r == null ? null : r.operaciones().get(clave);
        if (op == null && clave.equals(clavePago)) op = almacen.actualizar("reintegro:" + clavePago, Operacion.class, v -> v);
        return op == null ? null : op.resultado();
    }
    public void reiniciar() { almacen.limpiar("cobro:"); almacen.limpiar("reintegro:"); almacen.limpiar("reintegros-pago:"); almacen.limpiar("reintegro-vinculo:"); }
    private void compatible(Operacion r, BigDecimal importe) {
        if (r.importe().compareTo(importe) != 0) throw new IllegalStateException("Clave reutilizada con otro importe");
    }
    private void validar(String clave, BigDecimal importe) {
        validarClave(clave);
        if (importe == null || importe.signum() <= 0) throw new IllegalArgumentException("El importe debe ser mayor a cero");
    }
    private void validarClave(String clave) {
        if (clave == null || clave.isBlank() || clave.length() > 100) throw new IllegalArgumentException("La clave de idempotencia es obligatoria y admite hasta 100 caracteres");
    }
}
