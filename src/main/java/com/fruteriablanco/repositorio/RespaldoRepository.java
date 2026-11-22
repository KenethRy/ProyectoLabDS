package com.fruteriablanco.repositorio;

import com.fruteriablanco.modelo.Respaldo;
import com.fruteriablanco.util.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla respaldos (bitacora de respaldos y restauraciones). */
public class RespaldoRepository {

    public void registrar(int idUsuario, String tipo, String ruta, boolean exitoso) throws SQLException {
        String sql = "INSERT INTO respaldos (id_usuario, tipo, ruta_archivo, exitoso) VALUES (?, ?, ?, ?)";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, tipo);
            ps.setString(3, ruta.length() > 255 ? ruta.substring(0, 255) : ruta);
            ps.setBoolean(4, exitoso);
            ps.executeUpdate();
        }
    }

    public List<Respaldo> listarRecientes() throws SQLException {
        String sql = "SELECT r.id_respaldo, u.nombre, r.tipo, r.ruta_archivo, r.exitoso, r.fecha_operacion "
                + "FROM respaldos r JOIN usuarios u ON u.id_usuario = r.id_usuario "
                + "ORDER BY r.fecha_operacion DESC, r.id_respaldo DESC LIMIT 50";
        List<Respaldo> lista = new ArrayList<>();
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Respaldo r = new Respaldo();
                r.setIdRespaldo(rs.getInt(1));
                r.setUsuario(rs.getString(2));
                r.setTipo(rs.getString(3));
                r.setRutaArchivo(rs.getString(4));
                r.setExitoso(rs.getBoolean(5));
                r.setFecha(rs.getTimestamp(6).toLocalDateTime());
                lista.add(r);
            }
        }
        return lista;
    }
}
