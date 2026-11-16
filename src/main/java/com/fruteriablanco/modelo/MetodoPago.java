package com.fruteriablanco.modelo;

/** PAG-01: metodos de pago permitidos. El nombre debe coincidir con la tabla metodos_pago. */
public enum MetodoPago {
    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta"),
    TRANSFERENCIA("Transferencia");

    private final String nombre;

    MetodoPago(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
