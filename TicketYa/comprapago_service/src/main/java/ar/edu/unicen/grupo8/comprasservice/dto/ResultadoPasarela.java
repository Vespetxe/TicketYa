
package ar.edu.unicen.grupo8.comprasservice.dto;

public class ResultadoPasarela {

    private boolean aprobado;
    private String referenciaExterna;

    public ResultadoPasarela(boolean aprobado, String referenciaExterna) {
        this.aprobado = aprobado;
        this.referenciaExterna = referenciaExterna;
    }

    public boolean isAprobado() {
        return aprobado;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }
}
