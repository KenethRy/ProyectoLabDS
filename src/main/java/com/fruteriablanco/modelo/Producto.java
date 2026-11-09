package com.fruteriablanco.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Producto {

    public enum UnidadMedida { KG, PIEZA, CAJA, BOLSA }

    private int idProducto;
    private String codigo;
    private String nombre;
    private String categoria = "General";
    private String descripcion;
    private BigDecimal precio;
    private BigDecimal stock = BigDecimal.ZERO;
    private BigDecimal stockMinimo = BigDecimal.ZERO;
    private UnidadMedida unidadMedida = UnidadMedida.KG;
    private int diasAvisoVencimiento = 3;
    private boolean activo = true;
    private LocalDateTime fechaCreacion;

    public Producto() {
    }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public BigDecimal getStock() { return stock; }
    public void setStock(BigDecimal stock) { this.stock = stock; }

    public BigDecimal getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(BigDecimal stockMinimo) { this.stockMinimo = stockMinimo; }

    public UnidadMedida getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(UnidadMedida unidadMedida) { this.unidadMedida = unidadMedida; }

    public int getDiasAvisoVencimiento() { return diasAvisoVencimiento; }
    public void setDiasAvisoVencimiento(int diasAvisoVencimiento) { this.diasAvisoVencimiento = diasAvisoVencimiento; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    /** Regla de ALR-01: alerta cuando el stock es menor o igual al minimo. */
    public boolean tieneBajoStock() {
        return stock.compareTo(stockMinimo) <= 0;
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}
