package com.tidsec.novaeyetech_backend.util.pdf;

import java.awt.Color;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Lienzo de dibujo sobre PDFBox con origen arriba a la izquierda.
 *
 * <p>PDFBox mide la Y desde el borde inferior de la pagina; toda la maquetacion de este proyecto
 * razona desde el borde superior. Esta clase hace esa conversion en un solo lugar para que el codigo
 * de layout se lea como se ve la hoja, y encapsula ademas el ajuste de texto por ancho.
 */
public class PdfCanvas implements AutoCloseable {

    private static final float POINTS_PER_EM = 1000f;
    /** Factor de la curva de Bezier que aproxima un cuarto de circunferencia. */
    private static final float CIRCLE_KAPPA = 0.5523f;

    private final PDDocument document;
    private final float pageWidth;
    private final float pageHeight;

    private PDPageContentStream stream;

    public PdfCanvas(PDDocument document) {
        this.document = document;
        this.pageWidth = PDRectangle.A4.getWidth();
        this.pageHeight = PDRectangle.A4.getHeight();
    }

    public float pageWidth() {
        return pageWidth;
    }

    public float pageHeight() {
        return pageHeight;
    }

    public void newPage() {
        closeStream();

        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try {
            stream = new PDPageContentStream(document, page);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo abrir la pagina del PDF", ex);
        }
    }

    public void fillRect(float x, float top, float width, float height, Color color) {
        run(() -> {
            stream.setNonStrokingColor(color);
            stream.addRect(x, toPdfY(top + height), width, height);
            stream.fill();
        });
    }

    public void strokeRect(float x, float top, float width, float height, Color color, float lineWidth) {
        run(() -> {
            stream.setStrokingColor(color);
            stream.setLineWidth(lineWidth);
            stream.addRect(x, toPdfY(top + height), width, height);
            stream.stroke();
        });
    }

    /** Rectangulo redondeado relleno y con borde, equivalente a roundedRect + fillAndStroke. */
    public void roundedRect(float x, float top, float width, float height, float radius,
                            Color fill, Color border) {
        float bottom = toPdfY(top + height);
        float right = x + width;
        float upper = bottom + height;
        float offset = radius * CIRCLE_KAPPA;

        run(() -> {
            stream.setNonStrokingColor(fill);
            stream.setStrokingColor(border);
            stream.setLineWidth(1f);

            stream.moveTo(x + radius, upper);
            stream.lineTo(right - radius, upper);
            stream.curveTo(right - radius + offset, upper, right, upper - radius + offset, right, upper - radius);
            stream.lineTo(right, bottom + radius);
            stream.curveTo(right, bottom + radius - offset, right - radius + offset, bottom, right - radius, bottom);
            stream.lineTo(x + radius, bottom);
            stream.curveTo(x + radius - offset, bottom, x, bottom + radius - offset, x, bottom + radius);
            stream.lineTo(x, upper - radius);
            stream.curveTo(x, upper - radius + offset, x + radius - offset, upper, x + radius, upper);
            stream.closePath();
            stream.fillAndStroke();
        });
    }

    public void line(float x1, float top1, float x2, float top2, Color color, float lineWidth) {
        run(() -> {
            stream.setStrokingColor(color);
            stream.setLineWidth(lineWidth);
            stream.moveTo(x1, toPdfY(top1));
            stream.lineTo(x2, toPdfY(top2));
            stream.stroke();
        });
    }

    /** Dibuja una linea de texto cuyo borde superior queda en {@code top}. */
    public void text(String value, float x, float top, PDFont font, float fontSize, Color color) {
        if (value == null || value.isEmpty()) {
            return;
        }

        run(() -> {
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.setNonStrokingColor(color);
            stream.newLineAtOffset(x, toPdfY(top + fontSize));
            stream.showText(sanitize(value));
            stream.endText();
        });
    }

    public void textRight(String value, float right, float top, PDFont font, float fontSize, Color color) {
        text(value, right - textWidth(value, font, fontSize), top, font, fontSize, color);
    }

    public void textCentered(String value, float x, float width, float top,
                             PDFont font, float fontSize, Color color) {
        text(value, x + (width - textWidth(value, font, fontSize)) / 2, top, font, fontSize, color);
    }

    /** Dibuja el texto ajustado al ancho y devuelve la altura ocupada. */
    public float textWrapped(String value, float x, float top, float width,
                             PDFont font, float fontSize, float lineHeight, Color color) {
        List<String> lines = wrap(value, font, fontSize, width);
        float cursor = top;

        for (String line : lines) {
            text(line, x, cursor, font, fontSize, color);
            cursor += lineHeight;
        }

        return lines.size() * lineHeight;
    }

    public void drawImage(byte[] imageBytes, String name, float x, float top, float maxWidth, float maxHeight) {
        run(() -> {
            PDImageXObject image = PDImageXObject.createFromByteArray(document, imageBytes, name);
            float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
            float width = image.getWidth() * scale;
            float height = image.getHeight() * scale;

            stream.drawImage(image, x, toPdfY(top + height), width, height);
        });
    }

    public float textWidth(String value, PDFont font, float fontSize) {
        if (value == null || value.isEmpty()) {
            return 0f;
        }

        try {
            return font.getStringWidth(sanitize(value)) / POINTS_PER_EM * fontSize;
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo medir el texto del PDF", ex);
        }
    }

    /** Parte el texto en lineas que caben en {@code width}, cortando palabras solo si no caben solas. */
    public List<String> wrap(String value, PDFont font, float fontSize, float width) {
        List<String> lines = new ArrayList<>();

        if (value == null || value.isBlank()) {
            return lines;
        }

        StringBuilder current = new StringBuilder();

        for (String word : value.replaceAll("\\s+", " ").trim().split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;

            if (textWidth(candidate, font, fontSize) <= width) {
                current = new StringBuilder(candidate);
                continue;
            }

            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            current = new StringBuilder(truncateToWidth(word, font, fontSize, width, lines));
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }

        return lines;
    }

    @Override
    public void close() {
        closeStream();
    }

    /** Una palabra mas larga que el ancho disponible se parte en trozos que si caben. */
    private String truncateToWidth(String word, PDFont font, float fontSize, float width, List<String> lines) {
        StringBuilder chunk = new StringBuilder();

        for (char character : word.toCharArray()) {
            if (textWidth(chunk.toString() + character, font, fontSize) > width && !chunk.isEmpty()) {
                lines.add(chunk.toString());
                chunk = new StringBuilder();
            }
            chunk.append(character);
        }

        return chunk.toString();
    }

    /** Las fuentes Standard 14 solo cubren WinAnsi: cualquier otro caracter se sustituye. */
    private String sanitize(String value) {
        StringBuilder sanitized = new StringBuilder(value.length());

        for (char character : value.toCharArray()) {
            sanitized.append(character >= 32 && character <= 255 ? character : '?');
        }

        return sanitized.toString();
    }

    private float toPdfY(float top) {
        return pageHeight - top;
    }

    private void closeStream() {
        if (stream == null) {
            return;
        }

        try {
            stream.close();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo cerrar la pagina del PDF", ex);
        } finally {
            stream = null;
        }
    }

    private void run(DrawingAction action) {
        try {
            action.execute();
        } catch (IOException ex) {
            throw new UncheckedIOException("Error dibujando el PDF", ex);
        }
    }

    @FunctionalInterface
    private interface DrawingAction {
        void execute() throws IOException;
    }
}
