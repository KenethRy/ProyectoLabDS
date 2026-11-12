package com.fruteriablanco.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Venta {

    private int idVenta;
    private String folio;
    private int idUsuario;
    private final List<DetalleVenta> detalles = new ArrayList<>();

    public int getIdVenta() { return idVenta; }
    public void setIdVenta(int idVenta) { this.idVenta = idVenta; }

    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public List<DetalleVenta> getDetalles() { return detalles; }

    public BigDecimal getSubtotal() {
        BigDecimal suma = BigDecimal.ZERO;
        for (DetalleVenta d : detalles) {
            suma = suma.add(d.getSubtotal());
        }
        return suma;
    }

    /** Por ahora sin impuestos: el total es igual al subtotal. */
    public BigDecimal getTotal() {
        return getSubtotal();
    }
}