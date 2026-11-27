package com.fruteriablanco.presentacion;

import com.fruteriablanco.modelo.Usuario;
import com.fruteriablanco.util.Navegacion;
import com.fruteriablanco.util.SesionActual;
import com.fruteriablanco.util.TemaVisual;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

/**
 * Menu principal posterior al inicio de sesion (UI-01: interfaz sencilla).
 * Los accesos estan agrupados en Ventas, Inventario, Reportes y consultas y Administracion.
 * Cada rol ve solo las opciones que puede usar.
 * Atajos: Ctrl+1 Productos, Ctrl+2 Usuarios, Ctrl+3 Ventas, Ctrl+4 Alertas, Ctrl+5 Consultar ventas,
 * Ctrl+6 Respaldo, Ctrl+7 Bitacora, Ctrl+8 Reportes (Administrador).
 * Al abrir una ventana el menu se oculta y reaparece al volver (boton Volver o Ctrl+B).
 */
public class MenuPrincipalFrame extends JFrame {

    private static final Color VERDE = new Color(46, 125, 82);
    private static final Color FONDO = new Color(247, 250, 246);
    private static final Color LINEA = new Color(200, 215, 200);
    // Cada seccion tiene su propio color para ubicarse de un vistazo
    private static final Color C_VENTAS = VERDE;
    private static final Color C_INVENTARIO = new Color(196, 98, 16);
    private static final Color C_REPORTES = new Color(36, 113, 163);
    private static final Color C_ADMIN = new Color(102, 80, 150);

    private final JPanel panel = new JPanel(new GridBagLayout());
    private final GridBagConstraints gc = new GridBagConstraints();
    private int fila = 0;
    private Color colorSeccion = VERDE;
    private final java.util.List<JButton> botonesMenu = new java.util.ArrayList<>();

    public MenuPrincipalFrame() {
        super("Frutería Blanco");

        Usuario usuario = SesionActual.getUsuario();
        boolean admin = usuario.esAdministrador();
        boolean puedeVender = admin || "Empleado".equalsIgnoreCase(usuario.getRol().getNombre());

        panel.setBackground(FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 38, 22, 38));

        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

        // ---------- Encabezado con el logo ----------
        JPanel encabezado = new JPanel();
        encabezado.setLayout(new BoxLayout(encabezado, BoxLayout.Y_AXIS));
        encabezado.setBackground(FONDO);
        encabezado.setBorder(BorderFactory.createEmptyBorder(12, 20, 6, 20));
        JComponent logo = TemaVisual.logo(110);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel bienvenida = new JLabel("Bienvenido, " + usuario.getNombre());
        bienvenida.setFont(new Font("SansSerif", Font.BOLD, 16));
        bienvenida.setForeground(VERDE);
        bienvenida.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel rol = new JLabel("Rol: " + usuario.getRol().getNombre());
        rol.setFont(new Font("SansSerif", Font.PLAIN, 13));
        rol.setForeground(new Color(85, 95, 85));
        rol.setAlignmentX(Component.CENTER_ALIGNMENT);
        encabezado.add(logo);
        encabezado.add(Box.createVerticalStrut(6));
        encabezado.add(bienvenida);
        encabezado.add(rol);
        setIconImage(TemaVisual.icono());

        // Solo se muestran las opciones que el rol puede usar.
        // ---------- Ventas ----------
        if (puedeVender) {
            seccion("Ventas", C_VENTAS);
            agregar(opcion("Registrar venta", 3, KeyEvent.VK_3, VentaFrame::new));
        }

        // ---------- Inventario ----------
        seccion("Inventario", C_INVENTARIO);
        if (admin) {
            agregar(opcion("Registrar y administrar productos", 1, KeyEvent.VK_1, ProductoFrame::new));
        }
        agregar(opcion("Alertas de inventario", 4, KeyEvent.VK_4,
                () -> new AlertasFrame(usuario.getIdUsuario())));

        // ---------- Reportes y consultas ----------
        seccion("Reportes y consultas", C_REPORTES);
        agregar(opcion("Consultar ventas", 5, KeyEvent.VK_5, ConsultaVentasFrame::new));
        if (admin) {
            agregar(opcion("Reportes de ventas e inventario", 8, KeyEvent.VK_8, ReportesFrame::new));
            agregar(opcion("Bitácora de auditoría", 7, KeyEvent.VK_7, AuditoriaFrame::new));
        }

        // ---------- Administracion (solo Administrador) ----------
        if (admin) {
            seccion("Administración", C_ADMIN);
            agregar(opcion("Registrar y administrar usuarios", 2, KeyEvent.VK_2, UsuarioFrame::new));
            agregar(opcion("Respaldo y restauración", 6, KeyEvent.VK_6, RespaldoFrame::new));
        }

        // ---------- Salir ----------
        JButton btnSalir = new JButton("Cerrar sesion");
        btnSalir.setContentAreaFilled(false);
        btnSalir.setOpaque(true);
        btnSalir.setBorderPainted(false);
        btnSalir.setBackground(new Color(226, 230, 227));
        btnSalir.setForeground(new Color(60, 60, 60));
        btnSalir.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnSalir.setFocusPainted(false);
        btnSalir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSalir.addActionListener(e -> {
            SesionActual.cerrar();
            new LoginFrame().setVisible(true);
            dispose();
        });
        // El boton de cerrar sesion va fijo abajo, fuera del desplazamiento, para que siempre se vea
        JPanel barraInferior = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        barraInferior.setBackground(FONDO);
        barraInferior.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, LINEA));
        barraInferior.add(btnSalir);

        // Todos los botones del menu miden lo mismo que el mas ancho
        int anchoMax = 0;
        for (JButton b : botonesMenu) {
            anchoMax = Math.max(anchoMax, b.getPreferredSize().width);
        }
        for (JButton b : botonesMenu) {
            b.setPreferredSize(new Dimension(anchoMax, b.getPreferredSize().height));
        }

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(encabezado, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(FONDO);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        raiz.add(scroll, BorderLayout.CENTER);
        raiz.add(barraInferior, BorderLayout.SOUTH);
        setContentPane(raiz);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(460, 300));
        pack();
        Rectangle pantalla = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.max(getWidth(), 560), Math.min(getHeight(), pantalla.height));
        setLocationRelativeTo(null);
    }

    // ---------------------- Construccion del menu ----------------------

    /** Agrega un componente en la siguiente fila con el espacio indicado. */
    private void agregar(JComponent componente, Insets espacio) {
        gc.gridy = fila++;
        gc.insets = espacio;
        panel.add(componente, gc);
    }

    /** Agrega un boton con el espacio normal entre botones. */
    private void agregar(JButton boton) {
        agregarBoton(boton, new Insets(0, 0, 6, 0));
    }

    /** Boton del ancho de su texto, centrado (el color solo cubre el texto). */
    private void agregarBoton(JButton boton, Insets espacio) {
        botonesMenu.add(boton);
        gc.fill = GridBagConstraints.NONE;
        agregar(boton, espacio);
        gc.fill = GridBagConstraints.HORIZONTAL;
    }

    /** Titulo de grupo con una linea debajo. */
    private void seccion(String texto, Color color) {
        colorSeccion = color;
        JLabel etiqueta = new JLabel(texto.toUpperCase());
        etiqueta.setFont(new Font("SansSerif", Font.BOLD, 12));
        etiqueta.setForeground(color);
        etiqueta.setHorizontalAlignment(SwingConstants.CENTER);
        etiqueta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, LINEA),
                BorderFactory.createEmptyBorder(0, 0, 3, 0)));
        agregar(etiqueta, new Insets(14, 0, 6, 0));
    }

    /** Crea el boton de una opcion y registra su atajo Ctrl+numero. */
    private JButton opcion(String texto, int numero, int tecla, Supplier<JFrame> ventana) {
        JButton boton = new JButton(texto + "  (Ctrl+" + numero + ")");
        boton.setFont(new Font("SansSerif", Font.BOLD, 14));
        boton.setContentAreaFilled(false);
        boton.setOpaque(true);
        boton.setBorderPainted(false);
        boton.setBackground(colorSeccion);
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.setMargin(new Insets(6, 14, 6, 14));

        Runnable abrir = () -> Navegacion.abrir(this, ventana.get());
        boton.addActionListener(e -> abrir.run());
        Navegacion.agregarAtajo(getRootPane(), tecla, "opcion" + numero, abrir);
        return boton;
    }
}
