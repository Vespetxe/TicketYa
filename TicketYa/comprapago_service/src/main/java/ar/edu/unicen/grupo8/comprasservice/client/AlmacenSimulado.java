package ar.edu.unicen.grupo8.comprasservice.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

public interface AlmacenSimulado {
    <T> T actualizar(String clave, Class<T> tipo, UnaryOperator<T> accion);
    void limpiar(String prefijo);

    /** Para pruebas unitarias sin Spring. La aplicación utiliza el diario MySQL. */
    class Memoria implements AlmacenSimulado {
        private final ConcurrentHashMap<String, Object> datos = new ConcurrentHashMap<>();
        public <T> T actualizar(String clave, Class<T> tipo, UnaryOperator<T> accion) {
            return tipo.cast(datos.compute(clave, (k, v) -> accion.apply(tipo.cast(v))));
        }
        public void limpiar(String prefijo) { datos.keySet().removeIf(k -> k.startsWith(prefijo)); }
    }
}
