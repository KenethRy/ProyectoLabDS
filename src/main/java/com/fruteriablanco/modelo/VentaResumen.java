

package com.fruteriablanco.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Renglon de la consulta de ventas (BUS-02): una venta ya registrada. */
public class VentaResumen {

    private final int idVenta;
    private final String folio;
    private final LocalDateTime fecha;
    private final String vendedor;
    private final BigDecimal total;
    private final String estado;

    public VentaResumen(int idVenta, String folio, LocalDateTime fecha,
                        String vendedor, BigDecimal total, String estado) {
        this.idVenta = idVenta;
        this.folio = folio;
        this.fecha = fecha;
        this.vendedor = vendedor;
        this.total = total;
        this.estado = estado;
    }

    public int getIdVenta() { return idVenta; }
    public String getFolio() { return folio; }
    public LocalDateTime getFecha() { return fecha; }
    public String getVendedor() { return vendedor; }
    public BigDecimal getTotal() { return total; }
    public String getEstado() { return estado; }
}