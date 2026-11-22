package com.fruteriablanco.presentacion;

import com.fruteriablanco.modelo.Respaldo;
import com.fruteriablanco.modelo.Usuario;
import com.fruteriablanco.servicio.RespaldoException;
import com.fruteriablanco.servicio.RespaldoService;
import com.fruteriablanco.util.Navegacion;
import com.fruteriablanco.util.SesionActual;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/** Pantalla para respaldar y restaurar la base de datos (BD-01). Solo Administrador. */
public class RespaldoFrame extends JFrame {

    private static final Color VERDE = new Color(46, 125, 82);
    private static final Color FONDO = new Color(247, 250, 246);
    private static final Color ROJO = new Color(180, 30, 30);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter NOMBRE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final RespaldoService servicio = new RespaldoService();

    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Fecha", "Operación", "Usuario", "Resultado", "Archivo"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modelo);
    private final JLabel lblEstado = new JLabel(" ");
    private final JButton btnRespaldar = new JButton("Crear respaldo");
    private final JButton btnRestaurar = new JButton("Restaurar respaldo");
    private final JButton btnActualizar = new JButton("Actualizar");

    public RespaldoFrame() {
        super("Respaldo y restauración - Frutería Blanco");

        Usuario usuario = SesionActual.getUsuario();
        if (usuario == null || !usuario.esAdministrador()) {
            JOptionPane.showMessageDialog(null,
                    "Solo el rol Administrador puede respaldar o restaurar la base de datos.",
                    "Acceso restringido", JOptionPane.WARNING_MESSAGE);
            dispose();
            return;
        }

        construirInterfaz();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 520);
        setLocationRelativeTo(null);
        cargarHistorial();
    }

    private void construirInterfaz() {
        JPanel raiz = new JPanel(new BorderLayout(0, 12));
        raiz.setBackground(FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 20));

        JLabel titulo = new JLabel("Respaldo y restauración de la base de datos");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 22));
        titulo.setForeground(new Color(30, 90, 58));
        JLabel ayuda = new JLabel("Guarda una copia (.sql) de toda la información o recupérala desde un respaldo anterior.");
        ayuda.setForeground(new Color(95, 105, 95));
        JPanel encabezado = new JPanel(new GridLayout(2, 1, 0, 3));
        encabezado.setOpaque(false);
        encabezado.add(titulo);
        encabezado.add(ayuda);

        tabla.setRowHeight(24);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createTitledBorder("Historial de respaldos y restauraciones"));
        scroll.setOpaque(false);

        estilizar(btnRespaldar, VERDE, Color.WHITE);
        estilizar(btnRestaurar, new Color(230, 230, 230), new Color(60, 60, 60));
        estilizar(btnActualizar, new Color(230, 230, 230), new Color(60, 60, 60));
        btnRespaldar.addActionListener(e -> respaldar());
        btnRestaurar.addActionListener(e -> restaurar());
        btnActualizar.addActionListener(e -> cargarHistorial());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        botones.setOpaque(false);
        botones.add(Navegacion.botonVolver(this));
        botones.add(btnRespaldar);
        botones.add(btnRestaurar);
        botones.add(btnActualizar);

        lblEstado.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JPanel abajo = new JPanel(new BorderLayout(0, 8));
        abajo.setOpaque(false);
        abajo.add(botones, BorderLayout.NORTH);
        abajo.add(lblEstado, BorderLayout.SOUTH);

        raiz.add(encabezado, BorderLayout.NORTH);
        raiz.add(scroll, BorderLayout.CENTER);
        raiz.add(abajo, BorderLayout.SOUTH);
        setContentPane(raiz);
    }

    // ---------------------- Acciones ----------------------

    private void respaldar() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar respaldo");
        selector.setFileFilter(new FileNameExtensionFilter("Respaldo SQL (*.sql)", "sql"));
        selector.setSelectedFile(new java.io.File("respaldo_fruteria_" + LocalDateTime.now().format(NOMBRE) + ".sql"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destino = selector.getSelectedFile().toPath();
        if (!destino.getFileName().toString().toLowerCase().endsWith(".sql")) {
            destino = destino.resolveSibling(destino.getFileName() + ".sql");
        }
        if (destino.toFile().exists() && JOptionPane.showConfirmDialog(this,
                "Ya existe un archivo con ese nombre. ¿Reemplazarlo?", "Archivo existente",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        Path ruta = destino;
        ejecutar("Creando respaldo...", () -> {
            servicio.respaldar(ruta);
            return "Respaldo creado correctamente: " + ruta;
        });
    }

    private void restaurar() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Elegir respaldo a restaurar");
        selector.setFileFilter(new FileNameExtensionFilter("Respaldo SQL (*.sql)", "sql"));
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path archivo = selector.getSelectedFile().toPath();
        int respuesta = JOptionPane.showConfirmDialog(this,
                "Se reemplazará TODA la información actual por la del respaldo:\n" + archivo
                        + "\n\nLos datos registrados después de ese respaldo se perderán. ¿Continuar?",
                "Confirmar restauración", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }
        ejecutar("Restaurando respaldo, no cierres la aplicación...", () -> {
            servicio.restaurar(archivo);
            return "Restauración completada correctamente.";
        });
    }

    private void cargarHistorial() {
        try {
            modelo.setRowCount(0);
            for (Respaldo r : servicio.historial()) {
                modelo.addRow(new Object[]{
                        r.getFecha().format(FECHA),
                        "RESPALDO".equals(r.getTipo()) ? "Respaldo" : "Restauración",
                        r.getUsuario(),
                        r.isExitoso() ? "Exitoso" : "Fallido",
                        r.getRutaArchivo()});
            }
        } catch (RespaldoException e) {
            mostrar(e.getMessage(), ROJO);
        }
    }

    /** Corre la tarea fuera del hilo de pantalla para que la ventana no se congele. */
    private void ejecutar(String mensajeProceso, Callable<String> tarea) {
        alternarBotones(false);
        mostrar(mensajeProceso, new Color(85, 95, 85));
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return tarea.call();
            }

            @Override
            protected void done() {
                try {
                    mostrar(get(), VERDE);
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    mostrar(causa instanceof RespaldoException
                            ? causa.getMessage() : "Error inesperado: " + causa, ROJO);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                alternarBotones(true);
                cargarHistorial();
            }
        }.execute();
    }

    private void alternarBotones(boolean habilitados) {
        btnRespaldar.setEnabled(habilitados);
        btnRestaurar.setEnabled(habilitados);
        btnActualizar.setEnabled(habilitados);
    }

    private void mostrar(String texto, Color color) {
        lblEstado.setForeground(color);
        lblEstado.setText(texto);
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
