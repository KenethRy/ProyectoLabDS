package com.fruteriablanco.presentacion;

import com.fruteriablanco.modelo.Comprobante;
import com.fruteriablanco.repositorio.ComprobanteRepository;
import com.fruteriablanco.util.ComprobantePDF;
import com.fruteriablanco.util.Conexion;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.sql.Connection;

/** Vista previa del comprobante con opciones de imprimir o abrir el PDF. */
public class ComprobanteDialog extends JDialog {

    private final int idVenta;
    private final Path pdf;
    private final ComprobantePDF generador = new ComprobantePDF();

    /**
     * Punto de entrada: genera el PDF de la venta y muestra la vista previa.
     * Se llama despues de guardar la venta: ComprobanteDialog.emitir(this, idVenta);
     */
    public static void emitir(Component padre, int idVenta) {
        try {
            ComprobanteRepository repo = new ComprobanteRepository();
            Comprobante c;
            try (Connection con = Conexion.getConnection()) {
                c = repo.cargar(con, idVenta);
            }
            Path carpeta = Path.of(System.getProperty("user.home"), "FruteriaBlanco", "comprobantes");
            Path pdf = new ComprobantePDF().generar(c, carpeta);
            try (Connection con = Conexion.getConnection()) {
                repo.registrarTicket(con, idVenta, c.folio(), pdf.toString());
            }
            new ComprobanteDialog(SwingUtilities.getWindowAncestor(padre), idVenta, pdf, c.folio())
                    .setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(padre,
                    "No se pudo generar el comprobante:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ComprobanteDialog(Window padre, int idVenta, Path pdf, String folio) throws Exception {
        super(padre, "Comprobante " + folio, ModalityType.APPLICATION_MODAL);
        this.idVenta = idVenta;
        this.pdf = pdf;

        BufferedImage img;
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            img = new PDFRenderer(doc).renderImageWithDPI(0, 130);
        }
        JScrollPane scroll = new JScrollPane(new JLabel(new ImageIcon(img)));
        scroll.setPreferredSize(new Dimension(img.getWidth() + 30, Math.min(img.getHeight() + 10, 600)));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JButton btnImprimir = new JButton("Imprimir");
        JButton btnGuardar = new JButton("Guardar en la computadora");
        JButton btnCerrar = new JButton("Cerrar");
        btnImprimir.addActionListener(e -> imprimir());
        btnGuardar.addActionListener(e -> guardar());
        btnCerrar.addActionListener(e -> dispose());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        botones.add(btnImprimir);
        botones.add(btnGuardar);
        botones.add(btnCerrar);

        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(padre);
    }

    private void imprimir() {
        try {
            if (generador.imprimir(pdf)) {
                try (Connection con = Conexion.getConnection()) {
                    new ComprobanteRepository().marcarImpreso(con, idVenta);
                } catch (Exception ignorado) {
                    // Si falla el marcado, el comprobante ya se imprimio; no se interrumpe.
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo imprimir:\n" + ex.getMessage()
                            + "\n\nPuedes abrir el PDF y guardarlo o imprimirlo después.",
                    "Impresión", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void guardar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar comprobante");
        chooser.setSelectedFile(new File(pdf.getFileName().toString()));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivo PDF (*.pdf)", "pdf"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File destino = chooser.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".pdf")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".pdf");
        }

        if (destino.exists()) {
            int resp = JOptionPane.showConfirmDialog(this,
                    "El archivo ya existe. ¿Deseas reemplazarlo?",
                    "Guardar comprobante", JOptionPane.YES_NO_OPTION);
            if (resp != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            Files.copy(pdf, destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
            JOptionPane.showMessageDialog(this,
                    "Comprobante guardado en:\n" + destino.getAbsolutePath(),
                    "Guardar comprobante", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo guardar el comprobante:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}