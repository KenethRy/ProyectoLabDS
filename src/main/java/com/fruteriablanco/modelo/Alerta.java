package com.fruteriablanco.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Alerta {

    private final int idAlerta;
    private final String tipo;
    private final String estado;
    private final LocalDateTime fechaGenerada;
    private final String codigo;
    private final String nombreProducto;
    private final BigDecimal stock;
    private final BigDecimal stockMinimo;
    private final String unidadMedida;
    private final LocalDate fechaVencimiento;

    public Alerta(int idAlerta, String tipo, String estado, LocalDateTime fechaGenerada,
                  String codigo, String nombreProducto, BigDecimal stock, BigDecimal stockMinimo,
                  String unidadMedida, LocalDate fechaVencimiento) {
        this.idAlerta = idAlerta;
        this.tipo = tipo;
        this.estado = estado;
        this.fechaGenerada = fechaGenerada;
        this.codigo = codigo;
        this.nombreProducto = nombreProducto;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.unidadMedida = unidadMedida;
        this.fechaVencimiento = fechaVencimiento;
    }

    public int getIdAlerta() { return idAlerta; }
    public String getTipo() { return tipo; }
    public String getEstado() { return estado; }
    public LocalDateTime getFechaGenerada() { return fechaGenerada; }
    public String getCodigo() { return codigo; }
    public String getNombreProducto() { return nombreProducto; }
    public BigDecimal getStock() { return stock; }
    public BigDecimal getStockMinimo() { return stockMinimo; }
    public String getUnidadMedida() { return unidadMedida; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
}