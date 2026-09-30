package com.tidsec.novaeyetech_backend.util;

import com.tidsec.novaeyetech_backend.config.CompanyProperties;
import com.tidsec.novaeyetech_backend.model.Quotation;
import com.tidsec.novaeyetech_backend.model.QuotationDetail;
import com.tidsec.novaeyetech_backend.util.pdf.PdfCanvas;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Genera el PDF de una cotizacion.
 *
 * <p>Es una vista, no una regla de negocio: se limita a imprimir los importes ya congelados en la
 * cotizacion, sin recalcular nada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuotationPdfGenerator {

    private static final String LOGO_RESOURCE = "assets/logo-pdf.png";

    private static final Color PAGE_BACKGROUND = new Color(0xF2F2F2);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color HEADER_BACKGROUND = new Color(0x6A6A6A);
    // Color de marca (el mismo que --brand-600 del frontend) y su tinte claro para el encabezado de tabla.
    private static final Color ACCENT = new Color(0x374015);
    private static final Color BORDER = new Color(0xA6A6A6);
    private static final Color TEXT = Color.BLACK;
    private static final Color MUTED_TEXT = new Color(0x4D4D4D);
    private static final Color HEADER_TEXT = Color.WHITE;
    private static final Color TABLE_HEADER = new Color(0xC5CCA0);
    private static final Color FOOTER_TEXT = new Color(0x5C5C62);

    private static final float MARGIN = 36f;
    private static final float HEADER_HEIGHT = 128f;
    private static final float ACCENT_HEIGHT = 10f;
    /** Donde empieza el contenido bajo la cabecera; todo lo demas se mide desde aqui. */
    private static final float CONTENT_TOP = HEADER_HEIGHT + ACCENT_HEIGHT + 20f;
    private static final float LOGO_TOP = 8f;
    private static final float LOGO_MAX_WIDTH = 150f;
    private static final float LOGO_MAX_HEIGHT = HEADER_HEIGHT - 2 * LOGO_TOP;
    private static final float HEADER_GAP = 12f;
    private static final float CLIENT_CARD_HEIGHT = 92f;
    private static final float SUMMARY_HEIGHT = 108f;
    /** Alto minimo del cuadro de observaciones; crece con el texto. */
    private static final float OBSERVATIONS_HEIGHT = 88f;
    private static final float OBSERVATIONS_TEXT_TOP = 28f;
    private static final float OBSERVATIONS_LINE_HEIGHT = 12f;
    private static final float OBSERVATIONS_PADDING_BOTTOM = 12f;
    private static final float FOOTER_RESERVED = 56f;
    /** En paginas de continuacion el contenido baja para dejar lugar al rotulo "Cotizacion N". */
    private static final float CONTINUATION_CONTENT_TOP = CONTENT_TOP + 30f;
    private static final float ROW_MIN_HEIGHT = 28f;
    private static final float TABLE_HEADER_HEIGHT = 24f;

    private static final float COLUMN_QUANTITY = 52f;
    private static final float COLUMN_UNIT_PRICE = 96f;
    private static final float COLUMN_TOTAL = 98f;

    private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private final CompanyProperties company;

    public byte[] generate(Quotation quotation) {
        try (PDDocument document = new PDDocument();
             PdfCanvas canvas = new PdfCanvas(document);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            byte[] logo = readLogo();
            float contentWidth = canvas.pageWidth() - MARGIN * 2;

            canvas.newPage();
            drawPageChrome(canvas, quotation, logo, true);

            float cursor = drawClientCard(canvas, quotation, contentWidth);
            cursor = drawDetailTable(canvas, quotation, logo, contentWidth, cursor);
            cursor = drawSummary(canvas, quotation, logo, cursor);
            drawObservations(canvas, quotation, logo, contentWidth, cursor);
            drawFooter(canvas, contentWidth);

            canvas.close();
            document.save(output);

            return output.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo generar el PDF de la cotizacion", ex);
        }
    }

    /** Cabecera corporativa. En las paginas siguientes repite el numero de cotizacion. */
    private void drawPageChrome(PdfCanvas canvas, Quotation quotation, byte[] logo, boolean firstPage) {
        float pageWidth = canvas.pageWidth();

        canvas.fillRect(0, 0, pageWidth, canvas.pageHeight(), PAGE_BACKGROUND);
        canvas.fillRect(0, 0, pageWidth, HEADER_HEIGHT, HEADER_BACKGROUND);
        canvas.fillRect(0, HEADER_HEIGHT, pageWidth, ACCENT_HEIGHT, ACCENT);

        if (logo != null) {
            canvas.drawImage(logo, "logo", MARGIN, LOGO_TOP, LOGO_MAX_WIDTH, LOGO_MAX_HEIGHT);
        } else {
            canvas.text("NOVAEYE", MARGIN, 40f, bold, 20f, HEADER_TEXT);
            canvas.text("TECHNOLOGY S.A.S", MARGIN, 66f, bold, 11f, HEADER_TEXT);
        }

        float rightEdge = pageWidth - MARGIN;
        List<HeaderLine> details = List.of(
                new HeaderLine("No: " + quotation.getQuotationNumber(), 27f, 10.2f),
                new HeaderLine("Fecha: " + quotation.getIssuedAt(), 42f, 10.2f),
                new HeaderLine("RUC: " + company.taxId(), 60f, 8.9f),
                new HeaderLine("Direccion: " + company.address(), 74f, 8.9f),
                new HeaderLine("Telefono: " + company.phone(), 88f, 8.9f));

        float detailsWidth = 0f;
        for (HeaderLine line : details) {
            canvas.textRight(line.text(), rightEdge, line.top(), regular, line.size(), HEADER_TEXT);
            detailsWidth = Math.max(detailsWidth, canvas.textWidth(line.text(), regular, line.size()));
        }

        // El nombre ocupa el espacio entre el logo y los datos de la derecha; si no cabe, se achica
        // en lugar de pisarlos (un nombre o una direccion mas largos no rompen la cabecera).
        float titleX = MARGIN + LOGO_MAX_WIDTH + HEADER_GAP;
        float titleWidth = rightEdge - detailsWidth - HEADER_GAP - titleX;
        float titleSize = fitFontSize(canvas, company.name(), bold, 17f, 9f, titleWidth);
        float taglineSize = fitFontSize(canvas, company.tagline(), regular, 10.5f, 7f, titleWidth);

        canvas.textCentered(company.name(), titleX, titleWidth, 44f, bold, titleSize, HEADER_TEXT);
        canvas.textCentered(company.tagline(), titleX, titleWidth, 68f, regular, taglineSize,
                new Color(0xF5F5F5));

        if (!firstPage) {
            canvas.text("Cotizacion " + quotation.getQuotationNumber(), MARGIN, CONTENT_TOP - 4f, bold, 11f, TEXT);
        }
    }

    private record HeaderLine(String text, float top, float size) {
    }

    /** El mayor tamano entre {@code min} y {@code max} con el que el texto cabe en {@code width}. */
    private float fitFontSize(PdfCanvas canvas, String text, PDType1Font font, float max, float min, float width) {
        float size = max;
        while (size > min && canvas.textWidth(text, font, size) > width) {
            size -= 0.5f;
        }
        return size;
    }

    private float drawClientCard(PdfCanvas canvas, Quotation quotation, float contentWidth) {
        float top = CONTENT_TOP;

        canvas.roundedRect(MARGIN, top, contentWidth, CLIENT_CARD_HEIGHT, 8f, CARD_BACKGROUND, BORDER);

        var client = quotation.getClient();
        float leftX = MARGIN + 14;

        canvas.text("DATOS DEL CLIENTE", leftX, top + 12, bold, 11f, TEXT);
        canvas.text(client.getNameOrBusinessName(), leftX, top + 32, regular, 10f, MUTED_TEXT);
        canvas.text("Documento: " + client.getDocumentNumber(), leftX, top + 48, regular, 10f, MUTED_TEXT);
        canvas.text("Telefono: " + client.getPhone(), leftX, top + 62, regular, 10f, MUTED_TEXT);

        if (client.getEmail() != null) {
            canvas.text("Email: " + client.getEmail(), leftX, top + 76, regular, 10f, MUTED_TEXT);
        }

        float rightX = MARGIN + 320;

        canvas.text("Direccion: " + client.getAddress(), rightX, top + 32, regular, 10f, MUTED_TEXT);
        // Mismas alturas que la columna izquierda: con las anteriores el asesor pisaba el borde inferior.
        canvas.text("Ciudad: " + client.getCity(), rightX, top + 48, regular, 10f, MUTED_TEXT);
        canvas.text("Vigencia: " + quotation.getValidUntil(), rightX, top + 62, regular, 10f, MUTED_TEXT);
        canvas.text("Asesor: " + resolveAdvisor(quotation), rightX, top + 76, regular, 10f, MUTED_TEXT);

        return top + CLIENT_CARD_HEIGHT + 20;
    }

    private float drawDetailTable(PdfCanvas canvas, Quotation quotation, byte[] logo,
                                  float contentWidth, float startTop) {
        float descriptionWidth = Math.max(180f,
                contentWidth - COLUMN_QUANTITY - COLUMN_UNIT_PRICE - COLUMN_TOTAL);
        float rowBottomLimit = canvas.pageHeight() - FOOTER_RESERVED - 12;
        float cursor = drawTableHeader(canvas, contentWidth, descriptionWidth, startTop);
        String currency = quotation.getCurrency();
        int index = 0;

        for (QuotationDetail detail : quotation.getDetails()) {
            List<String> lines = canvas.wrap(detail.getDescriptionFrozen(), regular, 9f, descriptionWidth - 12);
            float rowHeight = Math.max(ROW_MIN_HEIGHT, lines.size() * 11f + 10f);

            if (cursor + rowHeight > rowBottomLimit) {
                canvas.newPage();
                drawPageChrome(canvas, quotation, logo, false);
                cursor = drawTableHeader(canvas, contentWidth, descriptionWidth, CONTINUATION_CONTENT_TOP);
            }

            if (index % 2 == 0) {
                canvas.fillRect(MARGIN, cursor, contentWidth, rowHeight, CARD_BACKGROUND);
            }
            canvas.strokeRect(MARGIN, cursor, contentWidth, rowHeight, BORDER, 0.5f);

            float descriptionX = MARGIN + COLUMN_QUANTITY;
            float unitPriceRight = descriptionX + descriptionWidth + COLUMN_UNIT_PRICE - 6;
            float totalRight = MARGIN + contentWidth - 6;

            canvas.textCentered(detail.getQuantity().toPlainString(), MARGIN, COLUMN_QUANTITY,
                    cursor + 8, regular, 9f, TEXT);
            canvas.textWrapped(detail.getDescriptionFrozen(), descriptionX + 6, cursor + 6,
                    descriptionWidth - 12, regular, 9f, 11f, TEXT);
            canvas.textRight(money(currency, detail.getUnitPriceFinal()), unitPriceRight,
                    cursor + 8, regular, 9f, TEXT);
            canvas.textRight(money(currency, detail.getLineTotal()), totalRight, cursor + 8, regular, 9f, TEXT);

            cursor += rowHeight;
            index++;
        }

        return cursor + 16;
    }

    private float drawTableHeader(PdfCanvas canvas, float contentWidth, float descriptionWidth, float top) {
        canvas.fillRect(MARGIN, top, contentWidth, TABLE_HEADER_HEIGHT, TABLE_HEADER);

        float descriptionX = MARGIN + COLUMN_QUANTITY;

        canvas.textCentered("CANT.", MARGIN, COLUMN_QUANTITY, top + 7, bold, 9f, TEXT);
        canvas.text("DESCRIPCION", descriptionX + 6, top + 7, bold, 9f, TEXT);
        canvas.textRight("P. UNITARIO", descriptionX + descriptionWidth + COLUMN_UNIT_PRICE - 6,
                top + 7, bold, 9f, TEXT);
        canvas.textRight("TOTAL", MARGIN + contentWidth - 6, top + 7, bold, 9f, TEXT);

        return top + TABLE_HEADER_HEIGHT;
    }

    private float drawSummary(PdfCanvas canvas, Quotation quotation, byte[] logo, float startTop) {
        float cursor = startTop;

        if (cursor + SUMMARY_HEIGHT + OBSERVATIONS_HEIGHT + 18 > canvas.pageHeight() - FOOTER_RESERVED - 12) {
            canvas.newPage();
            drawPageChrome(canvas, quotation, logo, false);
            cursor = CONTINUATION_CONTENT_TOP;
        }

        float boxWidth = 255f;
        float boxX = canvas.pageWidth() - MARGIN - boxWidth;
        float labelRight = boxX + 134;
        float valueRight = boxX + boxWidth - 12;
        String currency = quotation.getCurrency();

        canvas.roundedRect(boxX, cursor, boxWidth, SUMMARY_HEIGHT, 8f, PAGE_BACKGROUND, BORDER);

        float lineTop = cursor + 14;

        canvas.textRight("Subtotal:", labelRight, lineTop, regular, 10f, MUTED_TEXT);
        canvas.textRight(money(currency, linesTotal(quotation)), valueRight, lineTop, regular, 10f, MUTED_TEXT);

        canvas.textRight("Descuento:", labelRight, lineTop + 20, regular, 10f, MUTED_TEXT);
        canvas.textRight(money(currency, quotation.getDiscount()), valueRight, lineTop + 20,
                regular, 10f, MUTED_TEXT);

        canvas.textRight("IVA:", labelRight, lineTop + 40, regular, 10f, MUTED_TEXT);
        canvas.textRight(money(currency, quotation.getVatValueHistorical()), valueRight, lineTop + 40,
                regular, 10f, MUTED_TEXT);

        canvas.line(boxX + 12, lineTop + 62, boxX + boxWidth - 12, lineTop + 62, BORDER, 1f);

        canvas.textRight("TOTAL:", labelRight, lineTop + 72, bold, 12f, TEXT);
        canvas.textRight(money(currency, quotation.getTotal()), valueRight, lineTop + 72, bold, 12f, TEXT);

        return cursor + SUMMARY_HEIGHT + 18;
    }

    /**
     * Cuadro de observaciones. Respeta los saltos de linea que escribio el usuario y crece con el
     * texto; si no cabe en lo que queda de pagina, sigue en la siguiente en lugar de salirse del cuadro.
     */
    private void drawObservations(PdfCanvas canvas, Quotation quotation, byte[] logo,
                                  float contentWidth, float startTop) {
        String observations = quotation.getObservations() != null && !quotation.getObservations().isBlank()
                ? quotation.getObservations()
                : "Agradecemos la oportunidad de servirle. Esta cotizacion se elaboro segun los "
                        + "requerimientos levantados y mantiene vigencia segun fecha indicada.";

        List<String> lines = canvas.wrap(observations, regular, 9.5f, contentWidth - 24);
        // Mismo limite inferior que las filas de la tabla: deja libre el pie de pagina.
        float pageBottom = canvas.pageHeight() - FOOTER_RESERVED - 12;
        float cursor = startTop;
        int next = 0;
        boolean continued = false;

        while (next < lines.size()) {
            // El cuadro minimo no cabe en lo que queda: a la pagina siguiente.
            if (cursor + OBSERVATIONS_HEIGHT > pageBottom) {
                canvas.newPage();
                drawPageChrome(canvas, quotation, logo, false);
                cursor = CONTINUATION_CONTENT_TOP;
            }

            int count = Math.min(observationLinesThatFit(pageBottom - cursor), lines.size() - next);
            float height = Math.max(OBSERVATIONS_HEIGHT,
                    OBSERVATIONS_TEXT_TOP + count * OBSERVATIONS_LINE_HEIGHT + OBSERVATIONS_PADDING_BOTTOM);
            height = Math.min(height, pageBottom - cursor);

            canvas.roundedRect(MARGIN, cursor, contentWidth, height, 8f, CARD_BACKGROUND, BORDER);
            canvas.text(continued ? "OBSERVACIONES (continuacion)" : "OBSERVACIONES",
                    MARGIN + 12, cursor + 10, bold, 10f, TEXT);

            float lineTop = cursor + OBSERVATIONS_TEXT_TOP;
            for (String line : lines.subList(next, next + count)) {
                canvas.text(line, MARGIN + 12, lineTop, regular, 9.5f, MUTED_TEXT);
                lineTop += OBSERVATIONS_LINE_HEIGHT;
            }

            next += count;
            continued = true;
            // Fuerza el salto de pagina en la siguiente vuelta si aun queda texto.
            cursor = pageBottom;
        }
    }

    private int observationLinesThatFit(float availableHeight) {
        return (int) ((availableHeight - OBSERVATIONS_TEXT_TOP - OBSERVATIONS_PADDING_BOTTOM)
                / OBSERVATIONS_LINE_HEIGHT);
    }

    private void drawFooter(PdfCanvas canvas, float contentWidth) {
        float footerTop = canvas.pageHeight() - MARGIN - 26;

        canvas.textCentered(company.name() + " - " + company.tagline(), MARGIN, contentWidth,
                footerTop, regular, 8.5f, FOOTER_TEXT);
        canvas.textCentered("Documento generado automaticamente por " + company.name(), MARGIN, contentWidth,
                footerTop + 12, regular, 8.5f, FOOTER_TEXT);
    }

    /** El subtotal impreso es la suma de totales de linea (con IVA y margen), no la base sin IVA. */
    private BigDecimal linesTotal(Quotation quotation) {
        return quotation.getDetails().stream()
                .map(QuotationDetail::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String resolveAdvisor(Quotation quotation) {
        var author = quotation.getCreatedByUser();

        if (author == null) {
            return "Equipo comercial";
        }

        return author.getFullName() != null ? author.getFullName() : author.getEmail();
    }

    private String money(String currency, BigDecimal value) {
        return currency + " " + MoneyUtils.scale(value).toPlainString();
    }

    /** El logo es opcional: si falta, la cabecera cae al texto de la marca. */
    private byte[] readLogo() {
        try (InputStream input = new ClassPathResource(LOGO_RESOURCE).getInputStream()) {
            return input.readAllBytes();
        } catch (IOException ex) {
            log.warn("No se encontro el logo {} para el PDF, se usa el texto de marca", LOGO_RESOURCE);
            return null;
        }
    }
}
