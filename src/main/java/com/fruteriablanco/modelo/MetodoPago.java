package com.fruteriablanco.modelo;

/** Metodos de pago permitidos para registrar una venta. */
public enum MetodoPago {
    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta"),
    TRANSFERENCIA("Transferencia");

    private final String nombre;

    MetodoPago(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
