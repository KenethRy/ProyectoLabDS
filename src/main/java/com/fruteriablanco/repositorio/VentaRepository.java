
package com.fruteriablanco.repositorio;

import com.fruteriablanco.modelo.DetalleVenta;
import com.fruteriablanco.modelo.MetodoPago;
import com.fruteriablanco.modelo.Venta;
import com.fruteriablanco.util.Conexion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.fruteriablanco.modelo.VentaResumen;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
    public void guardar(Venta venta, MetodoPago metodoPago) throws SQLException {
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
                    insertarDetalle(con, idVenta, d);
                    actualizarStock(con, d.getIdProducto(), stockNuevo, venta.getIdUsuario());
                    generarAlertaStockBajo(con, d.getIdProducto());   // <-- solo esta es nueva
                    descontarLotes(con, d.getIdProducto(), d.getCantidad());
                    insertarMovimiento(con, venta, idVenta, idMotivo, d, stockAnterior, stockNuevo);
                    descontarLotes(con, d.getIdProducto(), d.getCantidad());
                    insertarMovimiento(con, venta, idVenta, idMotivo, d, stockAnterior, stockNuevo);
                }
                // PAG-01: el pago se guarda en la misma transaccion, asociado a esta venta
                insertarPago(con, idVenta, metodoPago, venta.getTotal());
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


    private void insertarPago(Connection con, int idVenta, MetodoPago metodoPago, BigDecimal monto)
            throws SQLException {
        String sql = """
                INSERT INTO pagos (id_venta, id_metodo_pago, monto)
                SELECT ?, id_metodo_pago, ? FROM metodos_pago WHERE nombre = ? AND activo = TRUE
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            ps.setBigDecimal(2, monto);
            ps.setString(3, metodoPago.getNombre());
            if (ps.executeUpdate() != 1) {
                throw new SQLException("El metodo de pago '" + metodoPago + "' no existe o esta inactivo.");
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




    private void generarAlertaStockBajo(Connection con, int idProducto) throws SQLException {
        String sql = """
            INSERT INTO alertas (id_producto, tipo_alerta, estado)
            SELECT p.id_producto, 'BAJO_STOCK', 'ACTIVA'
            FROM productos p
            WHERE p.id_producto = ?
              AND p.stock <= p.stock_minimo
              AND NOT EXISTS (
                  SELECT 1 FROM alertas a
                  WHERE a.id_producto = p.id_producto
                    AND a.tipo_alerta = 'BAJO_STOCK'
                    AND a.estado = 'ACTIVA')
            """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
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
    // ------------------------------------------------------------------
    // BUS-02: consultar ventas
    // ------------------------------------------------------------------

    /**
     * Busca ventas registradas. Todos los filtros son opcionales y se combinan:
     * desde/hasta (fechas incluidas), identificador (folio o numero de venta) y
     * producto (nombre o codigo). Devuelve las 200 ventas mas recientes que coincidan.
     */
    public List<VentaResumen> buscar(LocalDate desde, LocalDate hasta,
                                     String identificador, String producto) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT v.id_venta, v.folio, v.fecha_venta, u.nombre AS vendedor, v.total, v.estado
                FROM ventas v
                JOIN usuarios u ON u.id_usuario = v.id_usuario
                WHERE 1 = 1
                """);
        List<Object> parametros = new ArrayList<>();

        if (desde != null) {
            sql.append(" AND v.fecha_venta >= ?");
            parametros.add(Timestamp.valueOf(desde.atStartOfDay()));
        }
        if (hasta != null) {
            // hasta se incluye completo: se compara contra el inicio del dia siguiente
            sql.append(" AND v.fecha_venta < ?");
            parametros.add(Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
        }
        if (identificador != null && !identificador.isBlank()) {
            String id = identificador.trim();
            if (id.matches("\\d+")) {
                sql.append(" AND (v.id_venta = ? OR v.folio LIKE ?)");
                parametros.add(Integer.parseInt(id));
            } else {
                sql.append(" AND v.folio LIKE ?");
            }
            parametros.add("%" + id + "%");
        }
        if (producto != null && !producto.isBlank()) {
            sql.append("""
                     AND EXISTS (
                         SELECT 1 FROM detalle_venta d
                         JOIN productos p ON p.id_producto = d.id_producto
                         WHERE d.id_venta = v.id_venta
                           AND (p.nombre LIKE ? OR p.codigo LIKE ?))
                    """);
            String like = "%" + producto.trim() + "%";
            parametros.add(like);
            parametros.add(like);
        }
        sql.append(" ORDER BY v.fecha_venta DESC, v.id_venta DESC LIMIT 200");

        List<VentaResumen> resultado = new ArrayList<>();
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new VentaResumen(
                            rs.getInt("id_venta"),
                            rs.getString("folio"),
                            rs.getTimestamp("fecha_venta").toLocalDateTime(),
                            rs.getString("vendedor"),
                            rs.getBigDecimal("total"),
                            rs.getString("estado")));
                }
            }
        }
        return resultado;
    }

    /** Renglones (productos) de una venta ya registrada. */
    public List<DetalleVenta> detalleDe(int idVenta) throws SQLException {
        String sql = """
                SELECT d.id_producto, p.nombre, d.cantidad, d.precio_unitario
                FROM detalle_venta d
                JOIN productos p ON p.id_producto = d.id_producto
                WHERE d.id_venta = ?
                ORDER BY d.id_detalle
                """;
        List<DetalleVenta> detalles = new ArrayList<>();
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    detalles.add(new DetalleVenta(
                            rs.getInt("id_producto"),
                            rs.getString("nombre"),
                            rs.getBigDecimal("cantidad"),
                            rs.getBigDecimal("precio_unitario")));
                }
            }
        }
        return detalles;
    }
}