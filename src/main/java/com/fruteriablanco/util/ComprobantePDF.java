package com.fruteriablanco.util;

import com.fruteriablanco.modelo.Comprobante;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.printing.PDFPageable;

import javax.print.PrintServiceLookup;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Genera el comprobante como PDF tipo ticket (80 mm) y lo manda a imprimir. */
public class ComprobantePDF {

    private static final float ANCHO = 226.8f;      // 80 mm en puntos
    private static final float MARGEN = 10f;
    private static final float TAM = 8.5f;
    private static final float INTERLINEADO = 11f;
    private static final int COLS = 38;

    private static final PDType1Font NORMAL = new PDType1Font(Standard14Fonts.FontName.COURIER);
    private static final PDType1Font NEGRITA = new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);

    private record Renglon(String texto, boolean negrita) {
    }

    public Path generar(Comprobante c, Path carpeta) throws IOException {
        Files.createDirectories(carpeta);
        Path archivo = carpeta.resolve(c.folio().replaceAll("[^A-Za-z0-9_-]", "_") + ".pdf");

        List<Renglon> r = armarRenglones(c);
        float alto = 2 * MARGEN + r.size() * INTERLINEADO;

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(ANCHO, alto));
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = alto - MARGEN - TAM;
                for (Renglon linea : r) {
                    cs.beginText();
                    cs.setFont(linea.negrita() ? NEGRITA : NORMAL, TAM);
                    cs.newLineAtOffset(MARGEN, y);
                    cs.showText(limpiar(linea.texto()));
                    cs.endText();
                    y -= INTERLINEADO;
                }
            }
            doc.save(archivo.toFile());
        }
        return archivo;
    }

    public boolean imprimir(Path pdf) throws IOException, PrinterException {
        if (PrintServiceLookup.lookupPrintServices(null, null).length == 0) {
            throw new PrinterException("No hay ninguna impresora instalada o conectada.");
        }
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Comprobante de venta");
            job.setPageable(new PDFPageable(doc));
            if (!job.printDialog()) {
                return false;
            }
            job.print();
            return true;
        }
    }


    private List<Renglon> armarRenglones(Comprobante c) {
        List<Renglon> r = new ArrayList<>();
        String sep = "-".repeat(COLS);

        r.add(new Renglon(centrar("FRUTERÍA BLANCO"), true));
        r.add(new Renglon(centrar("Comprobante de venta"), false));
        r.add(new Renglon(sep, false));
        r.add(new Renglon("Folio:   " + c.folio(), true));
        r.add(new Renglon("Fecha:   " + c.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), false));
        r.add(new Renglon("Atendió: " + c.cajero(), false));
        r.add(new Renglon(sep, false));

        for (Comprobante.Linea l : c.lineas()) {
            for (String parte : partir(l.nombre(), COLS)) {
                r.add(new Renglon(parte, false));
            }
            String detalle = "  " + cantidad(l) + " x " + dinero(l.precioUnitario());
            r.add(new Renglon(izqDer(detalle, dinero(l.subtotal())), false));
        }

        r.add(new Renglon(sep, false));
        r.add(new Renglon(izqDer("Subtotal", dinero(c.subtotal())), false));
        r.add(new Renglon(izqDer("TOTAL", dinero(c.total())), true));
        r.add(new Renglon(sep, false));

        r.add(new Renglon("Método de pago:", true));
        for (Comprobante.Pago p : c.pagos()) {
            r.add(new Renglon(izqDer("  " + p.metodo(), dinero(p.monto())), false));
            if (p.referencia() != null && !p.referencia().isBlank()) {
                r.add(new Renglon("  Ref: " + p.referencia(), false));
            }
        }

        r.add(new Renglon(sep, false));
        r.add(new Renglon(centrar("¡Gracias por su compra!"), false));
        return r;
    }

    private static String cantidad(Comprobante.Linea l) {
        String unidad = switch (l.unidad() == null ? "" : l.unidad()) {
            case "KG" -> "kg";
            case "PIEZA" -> "pza";
            case "CAJA" -> "caja";
            case "BOLSA" -> "bolsa";
            default -> "";
        };
        return (l.cantidad().stripTrailingZeros().toPlainString() + " " + unidad).trim();
    }

    private static String dinero(BigDecimal v) {
        return String.format(Locale.US, "$%,.2f", v);
    }

    private static String centrar(String t) {
        int espacios = Math.max(0, (COLS - t.length()) / 2);
        return " ".repeat(espacios) + t;
    }

    private static String izqDer(String izq, String der) {
        int libre = COLS - der.length() - 1;
        if (izq.length() > libre) {
            izq = izq.substring(0, Math.max(0, libre));
        }
        return izq + " ".repeat(COLS - izq.length() - der.length()) + der;
    }

    private static List<String> partir(String texto, int ancho) {
        List<String> res = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.split(" ")) {
            while (palabra.length() > ancho) {
                if (actual.length() > 0) {
                    res.add(actual.toString());
                    actual.setLength(0);
                }
                res.add(palabra.substring(0, ancho));
                palabra = palabra.substring(ancho);
            }
            if (actual.length() + palabra.length() + (actual.length() > 0 ? 1 : 0) > ancho) {
                res.add(actual.toString());
                actual.setLength(0);
            }
            if (actual.length() > 0) {
                actual.append(' ');
            }
            actual.append(palabra);
        }
        if (actual.length() > 0) {
            res.add(actual.toString());
        }
        return res;
    }

    private static String limpiar(String t) {
        StringBuilder sb = new StringBuilder();
        for (char ch : t.toCharArray()) {
            sb.append(ch < 32 || ch > 255 ? '?' : ch);
        }
        return sb.toString();
    }
}
