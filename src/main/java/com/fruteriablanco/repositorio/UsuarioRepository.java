package com.fruteriablanco.repositorio;

import com.fruteriablanco.modelo.Rol;
import com.fruteriablanco.modelo.Usuario;
import com.fruteriablanco.util.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Optional;

/**
 * Capa de repositorio: único lugar donde se escribe SQL sobre usuarios.
 * No contiene reglas de negocio, solo consulta y guarda datos.
 */
public class UsuarioRepository {

    /** Busca un usuario por correo (con su rol). Devuelve vacío si no existe. */
    public Optional<Usuario> buscarPorCorreo(String correo) throws SQLException {
        String sql = """
                SELECT u.id_usuario, u.nombre, u.correo, u.contrasena_hash, u.activo,
                       u.fecha_creacion, r.id_rol, r.nombre AS rol_nombre, r.descripcion
                FROM usuarios u
                JOIN roles r ON r.id_rol = u.id_rol
                WHERE u.correo = ?
                """;

        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapear(rs));
            }
        }
    }

    /** Registra un intento de inicio de sesión. idUsuario es null si el correo no existe. */
    public void registrarAcceso(Integer idUsuario, String correoIntentado, boolean exitoso) throws SQLException {
        String sql = "INSERT INTO historial_acceso (id_usuario, correo_intentado, exitoso) VALUES (?, ?, ?)";

        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (idUsuario == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, idUsuario);
            }
            ps.setString(2, correoIntentado);
            ps.setBoolean(3, exitoso);
            ps.executeUpdate();
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Rol rol = new Rol(rs.getInt("id_rol"), rs.getString("rol_nombre"), rs.getString("descripcion"));

        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombre(rs.getString("nombre"));
        u.setCorreo(rs.getString("correo"));
        u.setContrasenaHash(rs.getString("contrasena_hash"));
        u.setActivo(rs.getBoolean("activo"));
        Timestamp creado = rs.getTimestamp("fecha_creacion");
        if (creado != null) {
            u.setFechaCreacion(creado.toLocalDateTime());
        }
        u.setRol(rol);
        return u;
    }
}
