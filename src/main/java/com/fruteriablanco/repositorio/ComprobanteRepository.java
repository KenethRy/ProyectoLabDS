package com.fruteriablanco.repositorio;

import com.fruteriablanco.modelo.Comprobante;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ComprobanteRepository {

    /** Lee de la BD todo lo que lleva el comprobante de una venta. */
    public Comprobante cargar(Connection con, int idVenta) throws SQLException {
        String folio;
        LocalDateTime fecha;
        String cajero;
        BigDecimal subtotal;
        BigDecimal total;

        String sqlVenta = "SELECT v.folio, v.fecha_venta, v.subtotal, v.total, u.nombre "
                + "FROM ventas v JOIN usuarios u ON u.id_usuario = v.id_usuario "
                + "WHERE v.id_venta = ?";
        try (PreparedStatement ps = con.prepareStatement(sqlVenta)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No existe la venta con id " + idVenta);
                }
                folio = rs.getString("folio");
                fecha = rs.getTimestamp("fecha_venta").toLocalDateTime();
                subtotal = rs.getBigDecimal("subtotal");
                total = rs.getBigDecimal("total");
                cajero = rs.getString("nombre");
            }
        }

        List<Comprobante.Linea> lineas = new ArrayList<>();
        String sqlDetalle = "SELECT p.nombre, p.unidad_medida, d.cantidad, d.precio_unitario, d.subtotal "
                + "FROM detalle_venta d JOIN productos p ON p.id_producto = d.id_producto "
                + "WHERE d.id_venta = ? ORDER BY d.id_detalle";
        try (PreparedStatement ps = con.prepareStatement(sqlDetalle)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lineas.add(new Comprobante.Linea(
                            rs.getString("nombre"),
                            rs.getString("unidad_medida"),
                            rs.getBigDecimal("cantidad"),
                            rs.getBigDecimal("precio_unitario"),
                            rs.getBigDecimal("subtotal")));
                }
            }
        }

        List<Comprobante.Pago> pagos = new ArrayList<>();
        String sqlPagos = "SELECT m.nombre, pg.monto, pg.referencia "
                + "FROM pagos pg JOIN metodos_pago m ON m.id_metodo_pago = pg.id_metodo_pago "
                + "WHERE pg.id_venta = ? ORDER BY pg.id_pago";
        try (PreparedStatement ps = con.prepareStatement(sqlPagos)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pagos.add(new Comprobante.Pago(
                            rs.getString("nombre"),
                            rs.getBigDecimal("monto"),
                            rs.getString("referencia")));
                }
            }
        }

        return new Comprobante(folio, fecha, cajero, lineas, pagos, subtotal, total);
    }

    /** Guarda (o actualiza) el registro del ticket en la tabla tickets. */
    public void registrarTicket(Connection con, int idVenta, String folio, String ruta) throws SQLException {
        String sql = "INSERT INTO tickets (id_venta, folio, ruta_archivo) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE ruta_archivo = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            ps.setString(2, folio);
            ps.setString(3, ruta);
            ps.setString(4, ruta);
            ps.executeUpdate();
        }
    }

    public void marcarImpreso(Connection con, int idVenta) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE tickets SET impreso = TRUE WHERE id_venta = ?")) {
            ps.setInt(1, idVenta);
            ps.executeUpdate();
        }
    }
}