package com.fruteriablanco.repositorio;

import com.fruteriablanco.util.Conexion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Consultas de solo lectura para la pantalla de reportes (UI-01). Solo cuenta ventas COMPLETADA. */
public class ReporteRepository {

    /** Ventas agrupadas por dia: {fecha (LocalDate), numero de ventas (Integer), total (BigDecimal)}. */
    public List<Object[]> ventasPorDia(LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = """
                SELECT DATE(fecha_venta) AS dia, COUNT(*) AS num, SUM(total) AS total
                FROM ventas
                WHERE estado = 'COMPLETADA' AND fecha_venta >= ? AND fecha_venta < ?
                GROUP BY DATE(fecha_venta)
                ORDER BY dia DESC
                """;
        List<Object[]> filas = new ArrayList<>();
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Object[]{rs.getDate("dia").toLocalDate(),
                            rs.getInt("num"), rs.getBigDecimal("total")});
                }
            }
        }
        return filas;
    }

    /** Productos mas vendidos: {nombre, cantidad (BigDecimal), importe (BigDecimal)}. Maximo 20. */
    public List<Object[]> productosMasVendidos(LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = """
                SELECT p.nombre, SUM(d.cantidad) AS cantidad, SUM(d.subtotal) AS importe
                FROM detalle_venta d
                JOIN ventas v ON v.id_venta = d.id_venta
                JOIN productos p ON p.id_producto = d.id_producto
                WHERE v.estado = 'COMPLETADA' AND v.fecha_venta >= ? AND v.fecha_venta < ?
                GROUP BY p.id_producto, p.nombre
                ORDER BY cantidad DESC
                LIMIT 20
                """;
        List<Object[]> filas = new ArrayList<>();
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Object[]{rs.getString("nombre"),
                            rs.getBigDecimal("cantidad"), rs.getBigDecimal("importe")});
                }
            }
        }
        return filas;
    }

    /** Inventario actual de productos activos: {nombre, stock, stock minimo} (BigDecimal). Los de stock bajo primero. */
    public List<Object[]> inventarioActual() throws SQLException {
        String sql = """
                SELECT nombre, stock, stock_minimo
                FROM productos
                WHERE activo = TRUE
                ORDER BY (stock <= stock_minimo) DESC, nombre
                """;
        List<Object[]> filas = new ArrayList<>();
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filas.add(new Object[]{rs.getString("nombre"),
                        rs.getBigDecimal("stock"), rs.getBigDecimal("stock_minimo")});
            }
        }
        return filas;
    }
}
