

package com.fruteriablanco.modelo;

import java.math.BigDecimal;

public class DetalleVenta {

    private int idProducto;
    private String nombreProducto;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;

    public DetalleVenta() {
    }

    public DetalleVenta(int idProducto, String nombreProducto,
                        BigDecimal cantidad, BigDecimal precioUnitario) {
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public int getIdProducto() { return idProducto; }
    public String getNombreProducto() { return nombreProducto; }
    public BigDecimal getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }

    /** Cantidad x precio, redondeado a 2 decimales. */
    public BigDecimal getSubtotal() {
        return cantidad.multiply(precioUnitario).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}