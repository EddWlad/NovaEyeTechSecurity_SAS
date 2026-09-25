package com.tidsec.novaeyetech_backend.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tidsec.novaeyetech_backend.exception.BusinessRuleException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PdfPageRendererTest {

    private final PdfPageRenderer renderer = new PdfPageRenderer();

    @Test
    @DisplayName("Devuelve un PNG por pagina, del ancho de una A4 a 130 DPI")
    void rendersOnePngPerPage() throws IOException {
        List<byte[]> pages = renderer.renderPages(pdfWithPages(2));

        assertThat(pages).hasSize(2);
        BufferedImage first = ImageIO.read(new ByteArrayInputStream(pages.get(0)));
        assertThat(first).isNotNull();
        // A4 = 8.27 pulgadas de ancho: 8.27 * 130 ~ 1075 px.
        assertThat(first.getWidth()).isBetween(1070, 1080);
    }

    @Test
    @DisplayName("Limita la vista previa a 20 paginas")
    void capsPageCount() throws IOException {
        assertThat(renderer.renderPages(pdfWithPages(23))).hasSize(20);
    }

    @Test
    @DisplayName("Un contenido que no es PDF se rechaza con un mensaje claro")
    void rejectsInvalidPdf() {
        assertThatThrownBy(() -> renderer.renderPages("no es un pdf".getBytes()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("vista previa");
    }

    private byte[] pdfWithPages(int count) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < count; i++) {
                document.addPage(new PDPage(PDRectangle.A4));
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
