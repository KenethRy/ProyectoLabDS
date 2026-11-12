package com.fruteriablanco.repositorio;

import com.fruteriablanco.modelo.Rol;
import com.fruteriablanco.modelo.Usuario;
import com.fruteriablanco.util.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Capa de repositorio: único lugar donde se escribe SQL sobre usuarios.
 * No contiene reglas de negocio, solo consulta y guarda datos.
 */
public class UsuarioRepository {

    private static final String SELECT_BASE = """
            SELECT u.id_usuario, u.nombre, u.correo, u.contrasena_hash, u.activo,
                   u.fecha_creacion, r.id_rol, r.nombre AS rol_nombre, r.descripcion
            FROM usuarios u
            JOIN roles r ON r.id_rol = u.id_rol
            """;

    /** Busca un usuario por correo (con su rol). Devuelve vacío si no existe. */
    public Optional<Usuario> buscarPorCorreo(String correo) throws SQLException {
        String sql = SELECT_BASE + " WHERE u.correo = ?";

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

    /** USR-01: consulta usuarios por nombre, correo o rol. Filtro vacío = todos. */
    public List<Usuario> listar(String filtro) throws SQLException {
        String sql = SELECT_BASE + """
                 WHERE ? = '' OR u.nombre LIKE ? OR u.correo LIKE ? OR r.nombre LIKE ?
                 ORDER BY u.activo DESC, u.nombre
                """;
        String f = filtro == null ? "" : filtro.trim();
        String like = "%" + f + "%";

        List<Usuario> usuarios = new ArrayList<>();
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, f);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    usuarios.add(mapear(rs));
                }
            }
        }
        return usuarios;
    }

    public List<Rol> listarRoles() throws SQLException {
        List<Rol> roles = new ArrayList<>();
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id_rol, nombre, descripcion FROM roles ORDER BY id_rol");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                roles.add(new Rol(rs.getInt("id_rol"), rs.getString("nombre"), rs.getString("descripcion")));
            }
        }
        return roles;
    }

    /** true si otro usuario (distinto de idExcluir) ya usa ese correo. */
    public boolean existeCorreo(String correo, int idExcluir) throws SQLException {
        String sql = "SELECT 1 FROM usuarios WHERE correo = ? AND id_usuario <> ?";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo);
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insertar(Usuario u, int idAdmin) throws SQLException {
        String sql = """
                INSERT INTO usuarios (nombre, correo, contrasena_hash, id_rol, creado_por)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection con = Conexion.getConnection()) {
            con.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, u.getNombre());
                    ps.setString(2, u.getCorreo());
                    ps.setString(3, u.getContrasenaHash());
                    ps.setInt(4, u.getRol().getIdRol());
                    ps.setInt(5, idAdmin);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        id = keys.getInt(1);
                    }
                }
                registrarAuditoria(con, idAdmin, "CREAR", id, null, json(u));
                con.commit();
                return id;
            } catch (SQLException | RuntimeException ex) {
                rollback(con, ex);
                throw ex;
            }
        }
    }

    /** Actualiza nombre, correo y rol. La contraseña solo cambia si u.getContrasenaHash() no es null. */
    public void actualizar(Usuario u, int idAdmin) throws SQLException {
        boolean cambiaClave = u.getContrasenaHash() != null;
        String sql = "UPDATE usuarios SET nombre = ?, correo = ?, id_rol = ?, actualizado_por = ?"
                + (cambiaClave ? ", contrasena_hash = ?" : "")
                + " WHERE id_usuario = ?";
        try (Connection con = Conexion.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    int i = 1;
                    ps.setString(i++, u.getNombre());
                    ps.setString(i++, u.getCorreo());
                    ps.setInt(i++, u.getRol().getIdRol());
                    ps.setInt(i++, idAdmin);
                    if (cambiaClave) {
                        ps.setString(i++, u.getContrasenaHash());
                    }
                    ps.setInt(i, u.getIdUsuario());
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("El usuario no existe.");
                    }
                }
                registrarAuditoria(con, idAdmin, "MODIFICAR", u.getIdUsuario(), null, json(u));
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                rollback(con, ex);
                throw ex;
            }
        }
    }

    /** Baja lógica: el usuario ya no puede iniciar sesión, pero se conserva su historial (ventas, accesos). */
    public void desactivar(int idUsuario, int idAdmin) throws SQLException {
        cambiarEstado(idUsuario, idAdmin, false);
    }

    public void activar(int idUsuario, int idAdmin) throws SQLException {
        cambiarEstado(idUsuario, idAdmin, true);
    }

    private void cambiarEstado(int idUsuario, int idAdmin, boolean activo) throws SQLException {
        String sql = activo
                ? "UPDATE usuarios SET activo = TRUE, fecha_baja = NULL, dado_de_baja_por = NULL, actualizado_por = ? WHERE id_usuario = ? AND activo = FALSE"
                : "UPDATE usuarios SET activo = FALSE, fecha_baja = CURRENT_TIMESTAMP, dado_de_baja_por = ? WHERE id_usuario = ? AND activo = TRUE";
        try (Connection con = Conexion.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, idAdmin);
                    ps.setInt(2, idUsuario);
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(activo
                                ? "El usuario ya estaba activo o no existe."
                                : "El usuario ya estaba inactivo o no existe.");
                    }
                }
                registrarAuditoria(con, idAdmin, activo ? "REACTIVAR" : "BAJA", idUsuario,
                        "{\"activo\":" + !activo + "}", "{\"activo\":" + activo + "}");
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                rollback(con, ex);
                throw ex;
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

    private void registrarAuditoria(Connection con, int idAdmin, String accion,
                                    int idRegistro, String anterior, String nuevo) throws SQLException {
        String sql = """
                INSERT INTO bitacora_auditoria
                    (id_usuario, accion, tabla_afectada, id_registro, valor_anterior, valor_nuevo)
                VALUES (?, ?, 'usuarios', ?, CAST(? AS JSON), CAST(? AS JSON))
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAdmin);
            ps.setString(2, accion);
            ps.setInt(3, idRegistro);
            ps.setString(4, anterior);
            ps.setString(5, nuevo);
            ps.executeUpdate();
        }
    }

    /** JSON para la bitácora. Nunca incluye la contraseña. */
    private String json(Usuario u) {
        return "{\"nombre\":\"" + escapar(u.getNombre()) + "\",\"correo\":\"" + escapar(u.getCorreo())
                + "\",\"rol\":\"" + escapar(u.getRol().getNombre()) + "\"}";
    }

    private String escapar(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void rollback(Connection con, Exception original) {
        try {
            con.rollback();
        } catch (SQLException rollback) {
            original.addSuppressed(rollback);
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
