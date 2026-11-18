package com.fruteriablanco.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Datos ya armados de una venta para imprimir o guardar como comprobante. */
public record Comprobante(
        String folio,
        LocalDateTime fecha,
        String cajero,
        List<Linea> lineas,
        List<Pago> pagos,
        BigDecimal subtotal,
        BigDecimal total) {

    public record Linea(String nombre, String unidad, BigDecimal cantidad,
                        BigDecimal precioUnitario, BigDecimal subtotal) {
    }

    public record Pago(String metodo, BigDecimal monto, String referencia) {
    }
}