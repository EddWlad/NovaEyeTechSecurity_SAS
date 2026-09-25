package com.tidsec.novaeyetech_backend.util;

import com.tidsec.novaeyetech_backend.exception.BusinessRuleException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

/**
 * Convierte las paginas de un PDF en imagenes PNG.
 *
 * <p>Existe para la vista previa en celulares: los navegadores moviles (Chrome en Android, por
 * ejemplo) no dibujan un PDF embebido en la pagina y muestran en su lugar una tarjeta generica. Una
 * imagen por pagina se ve igual en cualquier dispositivo y es exactamente el mismo documento que se
 * descarga.
 */
@Component
public class PdfPageRenderer {

    /** 130 DPI deja una hoja A4 en ~1075 px de ancho: nitida en pantallas de celular de alta densidad. */
    private static final float DPI = 130f;

    /** Tope de paginas por vista previa, para no generar decenas de imagenes con una cotizacion enorme. */
    private static final int MAX_PAGES = 20;

    public List<byte[]> renderPages(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pages = Math.min(document.getNumberOfPages(), MAX_PAGES);
            List<byte[]> images = new ArrayList<>(pages);

            for (int index = 0; index < pages; index++) {
                BufferedImage image = renderer.renderImageWithDPI(index, DPI, ImageType.RGB);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(image, "png", out);
                images.add(out.toByteArray());
            }

            return images;
        } catch (IOException ex) {
            throw new BusinessRuleException("No se pudo generar la vista previa del PDF");
        }
    }
}
