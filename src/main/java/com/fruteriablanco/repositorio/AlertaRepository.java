package com.fruteriablanco.repositorio;

import com.fruteriablanco.modelo.Alerta;
import com.fruteriablanco.util.Conexion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AlertaRepository {

    public List<Alerta> listarVigentes() throws SQLException {
        String sql = """
                SELECT a.id_alerta, a.tipo_alerta, a.estado, a.fecha_generada,
                       p.codigo, p.nombre, p.stock, p.stock_minimo, p.unidad_medida,
                       l.fecha_vencimiento
                FROM alertas a
                JOIN productos p ON p.id_producto = a.id_producto
                LEFT JOIN lotes l ON l.id_lote = a.id_lote
                WHERE a.estado IN ('ACTIVA', 'ATENDIDA')
                ORDER BY (a.estado = 'ACTIVA') DESC,
                         p.stock / NULLIF(p.stock_minimo, 0) ASC,
                         a.fecha_generada ASC
                """;
        List<Alerta> alertas = new ArrayList<>();
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Date venc = rs.getDate("fecha_vencimiento");
                alertas.add(new Alerta(
                        rs.getInt("id_alerta"),
                        rs.getString("tipo_alerta"),
                        rs.getString("estado"),
                        rs.getTimestamp("fecha_generada").toLocalDateTime(),
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getBigDecimal("stock"),
                        rs.getBigDecimal("stock_minimo"),
                        rs.getString("unidad_medida"),
                        venc == null ? null : venc.toLocalDate()));
            }
        }
        return alertas;
    }

    public int contarPendientes() throws SQLException {
        String sql = "SELECT COUNT(*) FROM alertas WHERE estado = 'ACTIVA'";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void marcarAtendida(int idAlerta, int idUsuario) throws SQLException {
        String sql = "UPDATE alertas SET estado = 'ATENDIDA', atendida_por = ? "
                + "WHERE id_alerta = ? AND estado = 'ACTIVA'";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idAlerta);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("La alerta ya fue atendida o ya no existe.");
            }
        }
    }
}