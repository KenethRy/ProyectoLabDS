package com.fruteriablanco.presentacion;

import com.fruteriablanco.repositorio.ReporteRepository;
import com.fruteriablanco.util.Navegacion;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Pantalla de reportes (UI-01): ventas por dia, productos mas vendidos e inventario actual. */
public class ReportesFrame extends JFrame {

    private static final Color VERDE = new Color(46, 125, 82);
    private static final Color FONDO = new Color(247, 250, 246);
    private static final Color ROJO = new Color(180, 30, 30);
    private static final Color GRIS = new Color(85, 95, 85);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReporteRepository repo = new ReporteRepository();

    private final JTextField txtDesde = new JTextField(9);
    private final JTextField txtHasta = new JTextField(9);
    private final JLabel lblMensaje = new JLabel(" ");

    private final DefaultTableModel mVentas = modelo("Fecha", "Ventas", "Total vendido");
    private final DefaultTableModel mProductos = modelo("Producto", "Cantidad vendida", "Importe");
    private final DefaultTableModel mInventario = modelo("Producto", "Stock actual", "Stock mínimo", "Estado");

    public ReportesFrame() {
        super("Reportes - Frutería Blanco");
        // Por defecto: ultimos 30 dias
        txtHasta.setText(LocalDate.now().format(FECHA));
        txtDesde.setText(LocalDate.now().minusDays(30).format(FECHA));

        JPanel raiz = new JPanel(new BorderLayout(0, 10));
        raiz.setBackground(FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        filtros.setOpaque(false);
        filtros.add(etiqueta("Desde:"));
        filtros.add(txtDesde);
        filtros.add(etiqueta("Hasta:"));
        filtros.add(txtHasta);
        JLabel formato = new JLabel("(dd/mm/aaaa)");
        formato.setForeground(GRIS);
        filtros.add(formato);
        JButton btnGenerar = new JButton("Generar");
        estilizar(btnGenerar, VERDE, Color.WHITE);
        btnGenerar.addActionListener(e -> generar());
        filtros.add(btnGenerar);
        filtros.add(Navegacion.botonVolver(this));
        txtDesde.addActionListener(e -> generar());
        txtHasta.addActionListener(e -> generar());

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Ventas por día", tabla(mVentas));
        pestanas.addTab("Productos más vendidos", tabla(mProductos));
        pestanas.addTab("Inventario actual", tabla(mInventario));

        lblMensaje.setFont(new Font("SansSerif", Font.PLAIN, 13));

        raiz.add(filtros, BorderLayout.NORTH);
        raiz.add(pestanas, BorderLayout.CENTER);
        raiz.add(lblMensaje, BorderLayout.SOUTH);
        setContentPane(raiz);

        generar();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(760, 560);
        setLocationRelativeTo(null);
    }

    private void generar() {
        LocalDate desde;
        LocalDate hasta;
        try {
            desde = LocalDate.parse(txtDesde.getText().trim(), FECHA);
            hasta = LocalDate.parse(txtHasta.getText().trim(), FECHA);
        } catch (DateTimeParseException e) {
            mensaje("Escribe las fechas como dd/mm/aaaa (por ejemplo 27/11/2026).", ROJO);
            return;
        }
        if (desde.isAfter(hasta)) {
            mensaje("La fecha \"Desde\" no puede ser posterior a \"Hasta\".", ROJO);
            return;
        }
        try {
            mVentas.setRowCount(0);
            BigDecimal total = BigDecimal.ZERO;
            int numVentas = 0;
            for (Object[] f : repo.ventasPorDia(desde, hasta)) {
                mVentas.addRow(new Object[]{((LocalDate) f[0]).format(FECHA), f[1], dinero((BigDecimal) f[2])});
                numVentas += (Integer) f[1];
                total = total.add((BigDecimal) f[2]);
            }

            mProductos.setRowCount(0);
            for (Object[] f : repo.productosMasVendidos(desde, hasta)) {
                mProductos.addRow(new Object[]{f[0],
                        ((BigDecimal) f[1]).stripTrailingZeros().toPlainString(), dinero((BigDecimal) f[2])});
            }

            mInventario.setRowCount(0);
            int bajos = 0;
            List<Object[]> inventario = repo.inventarioActual();
            for (Object[] f : inventario) {
                BigDecimal stock = (BigDecimal) f[1];
                BigDecimal minimo = (BigDecimal) f[2];
                boolean bajo = stock.compareTo(minimo) <= 0;
                if (bajo) bajos++;
                mInventario.addRow(new Object[]{f[0], stock.stripTrailingZeros().toPlainString(),
                        minimo.stripTrailingZeros().toPlainString(), bajo ? "Stock bajo" : "Normal"});
            }

            if (numVentas == 0) {
                mensaje("No hay ventas en ese periodo. Inventario: " + bajos + " producto(s) con stock bajo.", GRIS);
            } else {
                mensaje(numVentas + " venta(s) en el periodo, total " + dinero(total)
                        + ". Inventario: " + bajos + " producto(s) con stock bajo.", GRIS);
            }
        } catch (SQLException e) {
            mensaje("No se pudo generar el reporte: " + e.getMessage(), ROJO);
        }
    }

    private static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JScrollPane tabla(DefaultTableModel modelo) {
        JTable t = new JTable(modelo);
        t.setRowHeight(24);
        return new JScrollPane(t);
    }

    private void mensaje(String texto, Color color) {
        lblMensaje.setForeground(color);
        lblMensaje.setText(texto);
    }

    private JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        return l;
    }

    private String dinero(BigDecimal valor) {
        return String.format("$%.2f", valor == null ? BigDecimal.ZERO : valor);
    }

    private void estilizar(JButton boton, Color fondo, Color letra) {
        boton.setContentAreaFilled(false);
        boton.setOpaque(true);
        boton.setBorderPainted(false);
        boton.setBackground(fondo);
        boton.setForeground(letra);
        boton.setFont(new Font("SansSerif", Font.BOLD, 13));
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setMargin(new Insets(6, 14, 6, 14));
    }
}
