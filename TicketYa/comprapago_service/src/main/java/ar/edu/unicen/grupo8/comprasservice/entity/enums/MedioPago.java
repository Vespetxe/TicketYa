package ar.edu.unicen.grupo8.comprasservice.entity.enums;

/** Clasificación comercial; no contiene credenciales ni datos bancarios. */
public enum MedioPago {
    TARJETA_CREDITO, TARJETA_DEBITO, TRANSFERENCIA, BILLETERA_VIRTUAL;

    @com.fasterxml.jackson.annotation.JsonCreator
    public static MedioPago desdeJson(Object valor) {
        if (!(valor instanceof String texto)) throw new IllegalArgumentException("El medio de pago debe ser textual");
        return MedioPago.valueOf(texto);
    }
}
