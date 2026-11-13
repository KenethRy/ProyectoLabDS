
package com.fruteriablanco.repositorio;

import com.fruteriablanco.modelo.DetalleVenta;
import com.fruteriablanco.modelo.Venta;
import com.fruteriablanco.util.Conexion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Capa de repositorio de ventas: guarda la venta, sus renglones y descuenta
 * el inventario, todo en una sola transaccion (o se guarda todo o nada).
 */
public class VentaRepository {

    /**
     * Guarda la venta como COMPLETADA.
     * @throws IllegalArgumentException si un producto no existe o no alcanza el stock
     * @throws SQLException si falla la base de datos
     */
    public void guardar(Venta venta) throws SQLException {
        try (Connection con = Conexion.getConnection()) {
            con.setAutoCommit(false);
            try {
                int idVenta = insertarVenta(con, venta);
                venta.setIdVenta(idVenta);

                int idMotivo = obtenerIdMotivoVenta(con);

                for (DetalleVenta d : venta.getDetalles()) {
                    BigDecimal stockAnterior = bloquearYLeerStock(con, d);

                    if (d.getCantidad().compareTo(stockAnterior) > 0) {
                        throw new IllegalArgumentException("Stock insuficiente de "
                                + d.getNombreProducto() + " (disponible: " + stockAnterior + ").");
                    }
                    BigDecimal stockNuevo = stockAnterior.subtract(d.getCantidad());

                    insertarDetalle(con, idVenta, d);
                    actualizarStock(con, d.getIdProducto(), stockNuevo, venta.getIdUsuario());
                    descontarLotes(con, d.getIdProducto(), d.getCantidad());
                    insertarMovimiento(con, venta, idVenta, idMotivo, d, stockAnterior, stockNuevo);
                }
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                venta.setIdVenta(0);
                try {
                    con.rollback();
                } catch (SQLException rollback) {
                    ex.addSuppressed(rollback);
                }
                throw ex;
            }
        }
    }


    private int insertarVenta(Connection con, Venta venta) throws SQLException {
        String sql = """
                INSERT INTO ventas (folio, id_usuario, subtotal, total, estado)
                VALUES (?, ?, ?, ?, 'COMPLETADA')
                """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, venta.getFolio());
            ps.setInt(2, venta.getIdUsuario());
            ps.setBigDecimal(3, venta.getSubtotal());
            ps.setBigDecimal(4, venta.getTotal());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo obtener el id de la venta.");
    }

    private BigDecimal bloquearYLeerStock(Connection con, DetalleVenta d) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT stock FROM productos WHERE id_producto = ? AND activo = TRUE FOR UPDATE")) {
            ps.setInt(1, d.getIdProducto());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException(
                            "El producto " + d.getNombreProducto() + " no existe o esta inactivo.");
                }
                return rs.getBigDecimal("stock");
            }
        }
    }

    private void insertarDetalle(Connection con, int idVenta, DetalleVenta d) throws SQLException {
        String sql = """
                INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario, subtotal)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            ps.setInt(2, d.getIdProducto());
            ps.setBigDecimal(3, d.getCantidad());
            ps.setBigDecimal(4, d.getPrecioUnitario());
            ps.setBigDecimal(5, d.getSubtotal());
            ps.executeUpdate();
        }
    }

    private void actualizarStock(Connection con, int idProducto, BigDecimal stockNuevo, int idUsuario)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE productos SET stock = ?, actualizado_por = ? WHERE id_producto = ?")) {
            ps.setBigDecimal(1, stockNuevo);
            ps.setInt(2, idUsuario);
            ps.setInt(3, idProducto);
            ps.executeUpdate();
        }
    }

    /** Descuenta de los lotes mas antiguos primero (igual que ProductoRepository). */
    private void descontarLotes(Connection con, int idProducto, BigDecimal cantidad) throws SQLException {
        BigDecimal pendiente = cantidad;
        String sql = """
                SELECT id_lote, cantidad_actual FROM lotes
                WHERE id_producto = ? AND cantidad_actual > 0
                ORDER BY fecha_entrada ASC, id_lote ASC FOR UPDATE
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next() && pendiente.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal actual = rs.getBigDecimal("cantidad_actual");
                    BigDecimal descuento = actual.min(pendiente);
                    try (PreparedStatement upd = con.prepareStatement(
                            "UPDATE lotes SET cantidad_actual = ? WHERE id_lote = ?")) {
                        upd.setBigDecimal(1, actual.subtract(descuento));
                        upd.setInt(2, rs.getInt("id_lote"));
                        upd.executeUpdate();
                    }
                    pendiente = pendiente.subtract(descuento);
                }
            }
        }
    }

    private int obtenerIdMotivoVenta(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_motivo FROM motivos_movimiento WHERE nombre = 'Venta' AND tipo_movimiento = 'SALIDA' AND activo = TRUE");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        throw new SQLException("No se encontro el motivo 'Venta'. Verifica los datos iniciales de la base.");
    }

    private void insertarMovimiento(Connection con, Venta venta, int idVenta, int idMotivo,
                                    DetalleVenta d, BigDecimal anterior, BigDecimal nuevo) throws SQLException {
        String sql = """
                INSERT INTO movimientos_inventario
                    (id_producto, id_usuario, id_motivo, id_venta, tipo_movimiento, cantidad,
                     stock_anterior, stock_nuevo, referencia)
                VALUES (?, ?, ?, ?, 'SALIDA', ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, d.getIdProducto());
            ps.setInt(2, venta.getIdUsuario());
            ps.setInt(3, idMotivo);
            ps.setInt(4, idVenta);
            ps.setBigDecimal(5, d.getCantidad());
            ps.setBigDecimal(6, anterior);
            ps.setBigDecimal(7, nuevo);
            ps.setString(8, venta.getFolio());
            ps.executeUpdate();
        }
    }
}