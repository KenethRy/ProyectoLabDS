package com.fruteriablanco.presentacion;

import com.fruteriablanco.modelo.Usuario;
import com.fruteriablanco.util.Conexion;
import com.fruteriablanco.util.SesionActual;
import com.fruteriablanco.util.Navegacion;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Pantalla de consulta de auditoria e historial de precios. No permite editar ni borrar. */
public class AuditoriaFrame extends JFrame {
    private static final Color VERDE = new Color(46, 125, 82);
    private static final Color FONDO = new Color(247, 250, 246);

    private final JComboBox<UsuarioFiltro> filtroUsuario = new JComboBox<>();
    private final JComboBox<String> filtroAccion = new JComboBox<>(new String[]{"Todas", "CREAR", "MODIFICAR", "BAJA", "REACTIVAR", "LOGIN", "LOGOUT", "RESPALDAR", "RESTAURAR"});
    private final JTextField desde = new JTextField(10);
    private final JTextField hasta = new JTextField(10);
    private final DefaultTableModel modeloBitacora = modelo(new String[]{"Fecha", "Usuario", "Acción", "Tabla afectada", "ID registro", "Valor anterior", "Valor nuevo"});
    private final DefaultTableModel modeloPrecios = modelo(new String[]{"Fecha", "Producto", "Precio anterior", "Precio nuevo", "Usuario"});
    private JTable tablaBitacora;
    private JTable tablaPrecios;

    public AuditoriaFrame() {
        super("Bitácora de auditoría - Frutería Blanco");
        Usuario actual = SesionActual.getUsuario();
        if (actual == null || !actual.esAdministrador()) {
            JOptionPane.showMessageDialog(null, "Solo el Administrador puede consultar la bitácora.", "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            dispose();
            return;
        }
        construirInterfaz();
        cargarUsuarios();
        consultarBitacora();
        consultarPrecios();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 600));
        setSize(1180, 680);
        setLocationRelativeTo(null);
    }

    private void construirInterfaz() {
        JPanel principal = new JPanel(new BorderLayout(10, 10));
        principal.setBackground(FONDO);
        principal.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JLabel titulo = new JLabel("Bitácora de auditoría", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 23));
        titulo.setForeground(VERDE);
        principal.add(titulo, BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Bitácora de acciones", panelBitacora());
        pestanas.addTab("Historial de precios", panelPrecios());
        principal.add(pestanas, BorderLayout.CENTER);

        JButton volver = Navegacion.botonVolver(this);
        JPanel inferior = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        inferior.setOpaque(false);
        inferior.add(volver);
        principal.add(inferior, BorderLayout.SOUTH);
        setContentPane(principal);
    }

    private JPanel panelBitacora() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(FONDO);
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        filtros.setBackground(FONDO);
        filtros.add(new JLabel("Desde (AAAA-MM-DD):")); filtros.add(desde);
        filtros.add(new JLabel("Hasta:")); filtros.add(hasta);
        filtros.add(new JLabel("Usuario:")); filtros.add(filtroUsuario);
        filtros.add(new JLabel("Acción:")); filtros.add(filtroAccion);
        JButton buscar = new JButton("Filtrar"); estiloBoton(buscar); buscar.addActionListener(e -> consultarBitacora()); filtros.add(buscar);
        JButton limpiar = new JButton("Limpiar"); limpiar.addActionListener(e -> { desde.setText(""); hasta.setText(""); filtroUsuario.setSelectedIndex(0); filtroAccion.setSelectedIndex(0); consultarBitacora(); }); filtros.add(limpiar);
        tablaBitacora = new JTable(modeloBitacora);
        tablaBitacora.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        panel.add(filtros, BorderLayout.NORTH);
        panel.add(new JScrollPane(tablaBitacora), BorderLayout.CENTER);
        return panel;
    }

    private JPanel panelPrecios() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(FONDO);
        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT)); arriba.setOpaque(false);
        JButton actualizar = new JButton("Actualizar historial"); estiloBoton(actualizar); actualizar.addActionListener(e -> consultarPrecios());
        arriba.add(actualizar);
        tablaPrecios = new JTable(modeloPrecios);
        panel.add(arriba, BorderLayout.NORTH);
        panel.add(new JScrollPane(tablaPrecios), BorderLayout.CENTER);
        return panel;
    }

    private void cargarUsuarios() {
        filtroUsuario.addItem(new UsuarioFiltro(0, "Todos"));
        String sql = "SELECT id_usuario, nombre FROM usuarios ORDER BY nombre";
        try (Connection con = Conexion.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) filtroUsuario.addItem(new UsuarioFiltro(rs.getInt("id_usuario"), rs.getString("nombre")));
        } catch (SQLException ex) { mostrarError("No se pudieron cargar los usuarios", ex); }
    }

    private void consultarBitacora() {
        LocalDate f1, f2;
        try {
            f1 = desde.getText().isBlank() ? null : LocalDate.parse(desde.getText().trim());
            f2 = hasta.getText().isBlank() ? null : LocalDate.parse(hasta.getText().trim());
            if (f1 != null && f2 != null && f2.isBefore(f1)) throw new DateTimeParseException("Rango invertido", hasta.getText(), 0);
        } catch (DateTimeParseException ex) { JOptionPane.showMessageDialog(this, "Escribe las fechas con formato AAAA-MM-DD y verifica el rango.", "Fechas inválidas", JOptionPane.WARNING_MESSAGE); return; }

        limpiar(modeloBitacora);
        UsuarioFiltro u = (UsuarioFiltro) filtroUsuario.getSelectedItem();
        String accion = (String) filtroAccion.getSelectedItem();
        String sql = "SELECT b.fecha_accion, u.nombre, b.accion, b.tabla_afectada, b.id_registro, " +
                "CAST(b.valor_anterior AS CHAR) anterior, CAST(b.valor_nuevo AS CHAR) nuevo " +
                "FROM bitacora_auditoria b JOIN usuarios u ON u.id_usuario=b.id_usuario " +
                "WHERE (? IS NULL OR b.fecha_accion >= ?) AND (? IS NULL OR b.fecha_accion < DATE_ADD(?, INTERVAL 1 DAY)) " +
                "AND (?=0 OR b.id_usuario=?) AND (?='Todas' OR b.accion=?) ORDER BY b.fecha_accion DESC";
        try (Connection con = Conexion.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, f1 == null ? null : Date.valueOf(f1)); ps.setDate(2, f1 == null ? null : Date.valueOf(f1));
            ps.setDate(3, f2 == null ? null : Date.valueOf(f2)); ps.setDate(4, f2 == null ? null : Date.valueOf(f2));
            int id = u == null ? 0 : u.id; ps.setInt(5, id); ps.setInt(6, id);
            ps.setString(7, accion == null ? "Todas" : accion); ps.setString(8, accion == null ? "Todas" : accion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) modeloBitacora.addRow(new Object[]{rs.getTimestamp("fecha_accion"), rs.getString("nombre"), rs.getString("accion"), rs.getString("tabla_afectada"), rs.getObject("id_registro"), rs.getString("anterior"), rs.getString("nuevo")});
            }
            if (modeloBitacora.getRowCount() == 0) mensajeSinResultados(tablaBitacora);
        } catch (SQLException ex) { mostrarError("No se pudo consultar la bitácora", ex); }
    }

    private void consultarPrecios() {
        limpiar(modeloPrecios);
        String sql = "SELECT h.fecha_cambio, p.nombre producto, h.precio_anterior, h.precio_nuevo, u.nombre usuario " +
                "FROM historial_precios h JOIN productos p ON p.id_producto=h.id_producto " +
                "JOIN usuarios u ON u.id_usuario=h.id_usuario ORDER BY h.fecha_cambio DESC";
        try (Connection con = Conexion.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) modeloPrecios.addRow(new Object[]{rs.getTimestamp("fecha_cambio"), rs.getString("producto"), rs.getBigDecimal("precio_anterior"), rs.getBigDecimal("precio_nuevo"), rs.getString("usuario")});
            if (modeloPrecios.getRowCount() == 0) mensajeSinResultados(tablaPrecios);
        } catch (SQLException ex) { mostrarError("No se pudo consultar el historial de precios", ex); }
    }

    private static DefaultTableModel modelo(String[] columnas) { return new DefaultTableModel(columnas, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } }; }
    private static void limpiar(DefaultTableModel m) { m.setRowCount(0); }
    private static void estiloBoton(JButton b) { b.setBackground(VERDE); b.setForeground(Color.WHITE); b.setFocusPainted(false); b.setFont(new Font("SansSerif", Font.BOLD, 12)); }
    private void mensajeSinResultados(JTable tabla) { tabla.setToolTipText("No se encontraron registros"); JOptionPane.showMessageDialog(this, "No se encontraron registros", "Consulta", JOptionPane.INFORMATION_MESSAGE); }
    private void mostrarError(String mensaje, Exception ex) { JOptionPane.showMessageDialog(this, mensaje + ":\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
    private static class UsuarioFiltro { final int id; final String nombre; UsuarioFiltro(int id, String nombre) { this.id=id; this.nombre=nombre; } @Override public String toString() { return nombre; } }
}
