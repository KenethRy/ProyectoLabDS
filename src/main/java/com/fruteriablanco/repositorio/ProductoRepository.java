package com.fruteriablanco.repositorio;

import com.fruteriablanco.modelo.Producto;
import com.fruteriablanco.util.Conexion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Acceso a datos del catalogo y del inventario de productos. */
public class ProductoRepository {

    public List<Producto> listar(String filtro) throws SQLException {
        String sql = """
                SELECT id_producto, codigo, nombre, categoria, descripcion, precio, stock,
                       stock_minimo, unidad_medida, dias_aviso_vencimiento, activo, fecha_creacion
                FROM productos
                WHERE (? = '' OR codigo LIKE ? OR nombre LIKE ? OR categoria LIKE ?)
                ORDER BY activo DESC, nombre ASC
                """;

        String texto = filtro == null ? "" : filtro.trim();
        String patron = "%" + texto + "%";
        List<Producto> productos = new ArrayList<>();

        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, texto);
            ps.setString(2, patron);
            ps.setString(3, patron);
            ps.setString(4, patron);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapear(rs));
                }
            }
        }
        return productos;
    }

    /** Inserta el producto y, si tiene existencia inicial, crea tambien su lote y movimiento. */
    public void insertar(Producto producto, BigDecimal stockInicial, String proveedor,
                         String telefonoProveedor, LocalDate vencimiento, int idUsuario) throws SQLException {
        String sql = """
                INSERT INTO productos
                    (codigo, nombre, categoria, descripcion, precio, stock, stock_minimo,
                     unidad_medida, dias_aviso_vencimiento, creado_por)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = Conexion.getConnection()) {
            con.setAutoCommit(false);
            try {
                int idProducto;
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, producto.getCodigo());
                    ps.setString(2, producto.getNombre());
                    ps.setString(3, producto.getCategoria());
                    ps.setString(4, producto.getDescripcion());
                    ps.setBigDecimal(5, producto.getPrecio());
                    ps.setBigDecimal(6, stockInicial);
                    ps.setBigDecimal(7, producto.getStockMinimo());
                    ps.setString(8, producto.getUnidadMedida().name());
                    ps.setInt(9, producto.getDiasAvisoVencimiento());
                    ps.setInt(10, idUsuario);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("MySQL no devolvio el ID del producto creado.");
                        }
                        idProducto = keys.getInt(1);
                    }
                }

                if (stockInicial.compareTo(BigDecimal.ZERO) > 0) {
                    Integer idProveedor = obtenerOCrearProveedor(con, proveedor, telefonoProveedor);
                    int idLote = insertarLote(con, idProducto, idProveedor, stockInicial, vencimiento);
                    int idMotivo = obtenerIdMotivo(con, "Compra a proveedor", "ENTRADA");
                    insertarMovimiento(con, idProducto, idUsuario, idMotivo, idLote, "ENTRADA",
                            stockInicial, BigDecimal.ZERO, stockInicial, "STOCK INICIAL",
                            "Existencia inicial al registrar el producto.");
                }

                registrarAuditoria(con, idUsuario, "CREAR", idProducto,
                        "{}", jsonProducto(producto, stockInicial));
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                rollback(con, ex);
                throw ex;
            }
        }
    }

    public void actualizar(Producto producto, int idUsuario) throws SQLException {
        String consultaAnterior = "SELECT codigo, nombre, categoria, precio, stock FROM productos WHERE id_producto = ? FOR UPDATE";
        String sql = """
                UPDATE productos
                SET codigo = ?, nombre = ?, categoria = ?, descripcion = ?, precio = ?,
                    stock_minimo = ?, unidad_medida = ?, dias_aviso_vencimiento = ?, actualizado_por = ?
                WHERE id_producto = ? AND activo = TRUE
                """;

        try (Connection con = Conexion.getConnection()) {
            con.setAutoCommit(false);
            try {
                String anterior;
                try (PreparedStatement ps = con.prepareStatement(consultaAnterior)) {
                    ps.setInt(1, producto.getIdProducto());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("El producto ya no existe o esta inactivo.");
                        }
                        anterior = "{\"codigo\":\"" + escaparJson(rs.getString("codigo"))
                                + "\",\"nombre\":\"" + escaparJson(rs.getString("nombre"))
                                + "\",\"categoria\":\"" + escaparJson(rs.getString("categoria"))
                                + "\",\"precio\":\"" + rs.getBigDecimal("precio")
                                + "\",\"stock\":\"" + rs.getBigDecimal("stock") + "\"}";
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, producto.getCodigo());
                    ps.setString(2, producto.getNombre());
                    ps.setString(3, producto.getCategoria());
                    ps.setString(4, producto.getDescripcion());
                    ps.setBigDecimal(5, producto.getPrecio());
                    ps.setBigDecimal(6, producto.getStockMinimo());
                    ps.setString(7, producto.getUnidadMedida().name());
                    ps.setInt(8, producto.getDiasAvisoVencimiento());
                    ps.setInt(9, idUsuario);
                    ps.setInt(10, producto.getIdProducto());
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("No fue posible guardar los cambios del producto.");
                    }
                }

                registrarAuditoria(con, idUsuario, "MODIFICAR", producto.getIdProducto(),
                        anterior, jsonProducto(producto, null));
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                rollback(con, ex);
                throw ex;
            }
        }
    }

    public void desactivar(int idProducto, int idUsuario) throws SQLException {
        String sql = """
                UPDATE productos
                SET activo = FALSE, fecha_baja = CURRENT_TIMESTAMP, dado_de_baja_por = ?
                WHERE id_producto = ? AND activo = TRUE
                """;
        try (Connection con = Conexion.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, idUsuario);
                    ps.setInt(2, idProducto);
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("El producto ya estaba inactivo o no existe.");
                    }
                }
                registrarAuditoria(con, idUsuario, "BAJA", idProducto, "{}", "{\"activo\":false}");
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                rollback(con, ex);
                throw ex;
            }
        }
    }

    // REACTIVAR PRODUCTO
    public void activar(int idProducto, int idUsuario) throws SQLException {

        String sql = """
            UPDATE productos
            SET activo = TRUE,
                fecha_baja = NULL,
                dado_de_baja_por = NULL,
                actualizado_por = ?
            WHERE id_producto = ? AND activo = FALSE
            """;

        try (Connection con = Conexion.getConnection()) {

            con.setAutoCommit(false);

            try {

                try (PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setInt(1, idUsuario);
                    ps.setInt(2, idProducto);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "El producto ya estaba activo o no existe."
                        );
                    }
                }

                registrarAuditoria(
                        con,
                        idUsuario,
                        "REACTIVAR",
                        idProducto,
                        "{\"activo\":false}",
                        "{\"activo\":true}"
                );

                con.commit();

            } catch (SQLException | RuntimeException ex) {

                rollback(con, ex);
                throw ex;
            }
        }
    }

    /** Registra entradas o salidas y actualiza el stock en una sola transaccion. */
    public void registrarMovimiento(int idProducto, String tipo, BigDecimal cantidad, String motivo,
                                    String proveedor, String telefonoProveedor, LocalDate vencimiento,
                                    String observaciones, int idUsuario) throws SQLException {
        try (Connection con = Conexion.getConnection()) {
            con.setAutoCommit(false);
            try {
                BigDecimal stockAnterior;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT stock FROM productos WHERE id_producto = ? AND activo = TRUE FOR UPDATE")) {
                    ps.setInt(1, idProducto);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("El producto no existe o esta inactivo.");
                        }
                        stockAnterior = rs.getBigDecimal("stock");
                    }
                }

                BigDecimal stockNuevo;
                Integer idLote = null;
                if ("ENTRADA".equals(tipo)) {
                    stockNuevo = stockAnterior.add(cantidad);
                    Integer idProveedor = obtenerOCrearProveedor(con, proveedor, telefonoProveedor);
                    idLote = insertarLote(con, idProducto, idProveedor, cantidad, vencimiento);
                } else if ("SALIDA".equals(tipo)) {
                    if (cantidad.compareTo(stockAnterior) > 0) {
                        throw new SQLException("La salida supera la existencia actual (" + stockAnterior + ").");
                    }
                    stockNuevo = stockAnterior.subtract(cantidad);
                    descontarLotes(con, idProducto, cantidad);
                } else {
                    throw new SQLException("Tipo de movimiento no valido.");
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE productos SET stock = ?, actualizado_por = ? WHERE id_producto = ?")) {
                    ps.setBigDecimal(1, stockNuevo);
                    ps.setInt(2, idUsuario);
                    ps.setInt(3, idProducto);
                    ps.executeUpdate();
                }

                int idMotivo = obtenerIdMotivo(con, motivo, tipo);
                insertarMovimiento(con, idProducto, idUsuario, idMotivo, idLote, tipo,
                        cantidad, stockAnterior, stockNuevo, null, observaciones);
                String anterior = "{\"stock\":\"" + stockAnterior + "\"}";
                String nuevo = "{\"stock\":\"" + stockNuevo + "\"}";
                registrarAuditoria(con, idUsuario, "MODIFICAR", idProducto, anterior, nuevo);
                con.commit();
            } catch (SQLException | RuntimeException ex) {
                rollback(con, ex);
                throw ex;
            }
        }
    }

    public List<String> listarMotivos(String tipo) throws SQLException {
        List<String> motivos = new ArrayList<>();
        String sql = "SELECT nombre FROM motivos_movimiento WHERE tipo_movimiento = ? AND activo = TRUE ORDER BY nombre";
        try (Connection con = Conexion.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    motivos.add(rs.getString("nombre"));
                }
            }
        }
        return motivos;
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setIdProducto(rs.getInt("id_producto"));
        p.setCodigo(rs.getString("codigo"));
        p.setNombre(rs.getString("nombre"));
        p.setCategoria(rs.getString("categoria"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setPrecio(rs.getBigDecimal("precio"));
        p.setStock(rs.getBigDecimal("stock"));
        p.setStockMinimo(rs.getBigDecimal("stock_minimo"));
        p.setUnidadMedida(Producto.UnidadMedida.valueOf(rs.getString("unidad_medida")));
        p.setDiasAvisoVencimiento(rs.getInt("dias_aviso_vencimiento"));
        p.setActivo(rs.getBoolean("activo"));
        if (rs.getTimestamp("fecha_creacion") != null) {
            p.setFechaCreacion(rs.getTimestamp("fecha_creacion").toLocalDateTime());
        }
        return p;
    }

    private Integer obtenerOCrearProveedor(Connection con, String nombre, String telefono) throws SQLException {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        String buscar = "SELECT id_proveedor FROM proveedores WHERE LOWER(nombre) = LOWER(?) ORDER BY id_proveedor LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(buscar)) {
            ps.setString(1, nombre.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    if (telefono != null && !telefono.isBlank()) {
                        try (PreparedStatement actualizar = con.prepareStatement(
                                "UPDATE proveedores SET telefono = ?, activo = TRUE WHERE id_proveedor = ?")) {
                            actualizar.setString(1, telefono.trim());
                            actualizar.setInt(2, id);
                            actualizar.executeUpdate();
                        }
                    }
                    return id;
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO proveedores (nombre, telefono, activo) VALUES (?, ?, TRUE)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre.trim());
            if (telefono == null || telefono.isBlank()) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, telefono.trim());
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No fue posible registrar el proveedor.");
    }

    private int insertarLote(Connection con, int idProducto, Integer idProveedor,
                             BigDecimal cantidad, LocalDate vencimiento) throws SQLException {
        String sql = """
                INSERT INTO lotes (id_producto, id_proveedor, fecha_entrada, fecha_vencimiento,
                                   cantidad_inicial, cantidad_actual)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idProducto);
            if (idProveedor == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, idProveedor);
            ps.setDate(3, Date.valueOf(LocalDate.now()));
            if (vencimiento == null) ps.setNull(4, Types.DATE);
            else ps.setDate(4, Date.valueOf(vencimiento));
            ps.setBigDecimal(5, cantidad);
            ps.setBigDecimal(6, cantidad);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No fue posible crear el lote del producto.");
    }

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
        // Puede existir inventario legado sin lotes; el stock general sigue siendo el dato oficial.
    }

    private int obtenerIdMotivo(Connection con, String nombre, String tipo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id_motivo FROM motivos_movimiento WHERE nombre = ? AND tipo_movimiento = ? AND activo = TRUE")) {
            ps.setString(1, nombre);
            ps.setString(2, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No se encontro el motivo de inventario: " + nombre + ". Verifica los datos iniciales de la base.");
    }

    private void insertarMovimiento(Connection con, int idProducto, int idUsuario, int idMotivo,
                                    Integer idLote, String tipo, BigDecimal cantidad,
                                    BigDecimal anterior, BigDecimal nuevo, String referencia,
                                    String observaciones) throws SQLException {
        String sql = """
                INSERT INTO movimientos_inventario
                    (id_producto, id_usuario, id_motivo, id_lote, tipo_movimiento, cantidad,
                     stock_anterior, stock_nuevo, referencia, observaciones)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            ps.setInt(2, idUsuario);
            ps.setInt(3, idMotivo);
            if (idLote == null) ps.setNull(4, Types.INTEGER);
            else ps.setInt(4, idLote);
            ps.setString(5, tipo);
            ps.setBigDecimal(6, cantidad);
            ps.setBigDecimal(7, anterior);
            ps.setBigDecimal(8, nuevo);
            if (referencia == null || referencia.isBlank()) ps.setNull(9, Types.VARCHAR);
            else ps.setString(9, referencia);
            if (observaciones == null || observaciones.isBlank()) ps.setNull(10, Types.VARCHAR);
            else ps.setString(10, observaciones.trim());
            ps.executeUpdate();
        }
    }

    private void registrarAuditoria(Connection con, int idUsuario, String accion,
                                    int idRegistro, String anterior, String nuevo) throws SQLException {
        String sql = """
                INSERT INTO bitacora_auditoria
                    (id_usuario, accion, tabla_afectada, id_registro, valor_anterior, valor_nuevo)
                VALUES (?, ?, 'productos', ?, CAST(? AS JSON), CAST(? AS JSON))
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, accion);
            ps.setInt(3, idRegistro);
            ps.setString(4, anterior);
            ps.setString(5, nuevo);
            ps.executeUpdate();
        }
    }

    private String jsonProducto(Producto p, BigDecimal stock) {
        return "{\"codigo\":\"" + escaparJson(p.getCodigo())
                + "\",\"nombre\":\"" + escaparJson(p.getNombre())
                + "\",\"categoria\":\"" + escaparJson(p.getCategoria())
                + "\",\"precio\":\"" + p.getPrecio()
                + "\",\"stock\":\"" + (stock == null ? "no modificado" : stock)
                + "\",\"unidad_medida\":\"" + p.getUnidadMedida().name() + "\"}";
    }

    private String escaparJson(String valor) {
        if (valor == null) return "";
        return valor.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private void rollback(Connection con, Exception original) {
        try {
            con.rollback();
        } catch (SQLException rollback) {
            original.addSuppressed(rollback);
        }
    }
}
