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
    private static final float HEADER_HEIGHT = 110f;
    private static final float ACCENT_HEIGHT = 10f;
    private static final float CLIENT_CARD_HEIGHT = 92f;
    private static final float SUMMARY_HEIGHT = 108f;
    private static final float OBSERVATIONS_HEIGHT = 88f;
    private static final float FOOTER_RESERVED = 56f;
    private static final float CONTINUATION_CONTENT_TOP = 170f;
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

        float logoX = MARGIN - 2;

        if (logo != null) {
            canvas.drawImage(logo, "logo", logoX, 10f, 138f, 92f);
        } else {
            canvas.text("NOVAEYE", logoX, 30f, bold, 20f, HEADER_TEXT);
            canvas.text("TECHNOLOGY S.A.S", logoX, 56f, bold, 11f, HEADER_TEXT);
        }

        float companyInfoX = MARGIN + 122;
        float companyInfoWidth = 220f;

        // 15 pt: el nombre actual es largo y a 17 pt quedaba pegado a los datos de la derecha.
        canvas.textCentered(company.name(), companyInfoX, companyInfoWidth, 31f, bold, 15f, HEADER_TEXT);
        canvas.textCentered(company.tagline(), companyInfoX, companyInfoWidth, 54f, regular, 10.5f,
                new Color(0xF5F5F5));

        float rightEdge = pageWidth - MARGIN;

        canvas.textRight("No: " + quotation.getQuotationNumber(), rightEdge, 18f, regular, 10.2f, HEADER_TEXT);
        canvas.textRight("Fecha: " + quotation.getIssuedAt(), rightEdge, 33f, regular, 10.2f, HEADER_TEXT);
        canvas.textRight("RUC: " + company.taxId(), rightEdge, 51f, regular, 8.9f, HEADER_TEXT);
        canvas.textRight("Direccion: " + company.address(), rightEdge, 65f, regular, 8.9f, HEADER_TEXT);
        canvas.textRight("Telefono: " + company.phone(), rightEdge, 79f, regular, 8.9f, HEADER_TEXT);

        if (!firstPage) {
            canvas.text("Cotizacion " + quotation.getQuotationNumber(), MARGIN, 136f, bold, 11f, TEXT);
        }
    }

    private float drawClientCard(PdfCanvas canvas, Quotation quotation, float contentWidth) {
        float top = 140f;

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

        if (cursor + SUMMARY_HEIGHT + OBSERVATIONS_HEIGHT + 18 > canvas.pageHeight() - FOOTER_RESERVED) {
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

    private void drawObservations(PdfCanvas canvas, Quotation quotation, byte[] logo,
                                  float contentWidth, float startTop) {
        float cursor = startTop;

        if (cursor + OBSERVATIONS_HEIGHT > canvas.pageHeight() - FOOTER_RESERVED) {
            canvas.newPage();
            drawPageChrome(canvas, quotation, logo, false);
            cursor = CONTINUATION_CONTENT_TOP;
        }

        canvas.roundedRect(MARGIN, cursor, contentWidth, OBSERVATIONS_HEIGHT, 8f, CARD_BACKGROUND, BORDER);
        canvas.text("OBSERVACIONES", MARGIN + 12, cursor + 10, bold, 10f, TEXT);

        String observations = quotation.getObservations() != null && !quotation.getObservations().isBlank()
                ? quotation.getObservations()
                : "Agradecemos la oportunidad de servirle. Esta cotizacion se elaboro segun los "
                        + "requerimientos levantados y mantiene vigencia segun fecha indicada.";

        canvas.textWrapped(observations, MARGIN + 12, cursor + 28, contentWidth - 24,
                regular, 9.5f, 12f, MUTED_TEXT);
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
