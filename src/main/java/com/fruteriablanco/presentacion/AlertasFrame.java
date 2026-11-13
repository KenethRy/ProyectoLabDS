package com.fruteriablanco.presentacion;

import com.fruteriablanco.modelo.Alerta;
import com.fruteriablanco.servicio.AlertaService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AlertasFrame extends JFrame {

    private static final Color VERDE = new Color(46, 125, 82);
    private static final Color FONDO = new Color(247, 250, 246);
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AlertaService servicio = new AlertaService();
    private final int idUsuario;
    private List<Alerta> alertas = new ArrayList<>();

    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Alerta", "Código", "Producto", "Stock", "Mínimo", "Estado", "Desde"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modelo);
    private final JLabel lblResumen = new JLabel(" ");
    private final JLabel lblMensaje = new JLabel(" ");

    public AlertasFrame(int idUsuario) {
        super("Alertas de inventario - Frutería Blanco");
        this.idUsuario = idUsuario;
        construirInterfaz();
        cargar();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(820, 450);
        setLocationRelativeTo(null);
    }

    private void construirInterfaz() {
        JPanel raiz = new JPanel(new BorderLayout(0, 10));
        raiz.setBackground(FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        lblResumen.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblMensaje.setForeground(new Color(180, 30, 30));

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(24);
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                           boolean foco, int fila, int columna) {
                Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
                if (!seleccionada && fila < alertas.size()) {
                    c.setBackground(colorFila(alertas.get(fila)));
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createTitledBorder("Alertas vigentes"));
        scroll.setOpaque(false);

        JButton btnActualizar = new JButton("Actualizar");
        estilizar(btnActualizar, new Color(230, 230, 230), new Color(60, 60, 60));
        btnActualizar.addActionListener(e -> cargar());

        JButton btnAtender = new JButton("Marcar como atendida");
        estilizar(btnAtender, VERDE, Color.WHITE);
        btnAtender.addActionListener(e -> atender());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnActualizar);
        botones.add(btnAtender);

        JPanel abajo = new JPanel(new BorderLayout());
        abajo.setOpaque(false);
        abajo.add(lblMensaje, BorderLayout.CENTER);
        abajo.add(botones, BorderLayout.EAST);

        raiz.add(lblResumen, BorderLayout.NORTH);
        raiz.add(scroll, BorderLayout.CENTER);
        raiz.add(abajo, BorderLayout.SOUTH);
        setContentPane(raiz);
    }


    private void cargar() {
        try {
            alertas = servicio.listarVigentes();
            modelo.setRowCount(0);
            long pendientes = 0;
            for (Alerta a : alertas) {
                if ("ACTIVA".equals(a.getEstado())) pendientes++;
                modelo.addRow(new Object[]{
                        etiquetaTipo(a), a.getCodigo(), a.getNombreProducto(),
                        cantidad(a.getStock()) + " " + a.getUnidadMedida(),
                        cantidad(a.getStockMinimo()),
                        "ACTIVA".equals(a.getEstado()) ? "Pendiente" : "Atendida",
                        a.getFechaGenerada().format(FORMATO)
                });
            }
            lblResumen.setText(pendientes == 0
                    ? "Sin alertas pendientes."
                    : pendientes + " alerta(s) pendiente(s)");
            lblMensaje.setText(" ");
        } catch (SQLException e) {
            lblMensaje.setText("No se pudieron cargar las alertas: " + e.getMessage());
        }
    }

    private void atender() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            lblMensaje.setText("Selecciona una alerta para marcarla como atendida.");
            return;
        }
        try {
            servicio.marcarAtendida(alertas.get(fila).getIdAlerta(), idUsuario);
            cargar();
        } catch (SQLException | IllegalArgumentException e) {
            lblMensaje.setText(e.getMessage());
        }
    }

    private String etiquetaTipo(Alerta a) {
        return switch (a.getTipo()) {
            case "BAJO_STOCK" -> a.getStock().signum() <= 0 ? "Agotado" : "Bajo stock";
            case "POR_VENCER" -> "Por vencer";
            case "VENCIDO" -> "Vencido";
            default -> a.getTipo();
        };
    }

    private Color colorFila(Alerta a) {
        if ("ATENDIDA".equals(a.getEstado())) return new Color(238, 238, 238);
        boolean critica = "VENCIDO".equals(a.getTipo())
                || ("BAJO_STOCK".equals(a.getTipo()) && a.getStock().signum() <= 0);
        return critica ? new Color(255, 205, 210) : new Color(255, 236, 179);
    }

    private String cantidad(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
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