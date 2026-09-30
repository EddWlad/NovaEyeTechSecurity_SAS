package com.tidsec.novaeyetech_backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.tidsec.novaeyetech_backend.config.CompanyProperties;
import com.tidsec.novaeyetech_backend.model.Client;
import com.tidsec.novaeyetech_backend.model.Quotation;
import com.tidsec.novaeyetech_backend.model.QuotationDetail;
import com.tidsec.novaeyetech_backend.util.pdf.PdfCanvas;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QuotationPdfGeneratorTest {

    private final QuotationPdfGenerator generator = new QuotationPdfGenerator(new CompanyProperties(
            "NOVAEYE TECHNOLOGY S.A.S", "Tu aliado en seguridad", "1793241347001",
            "De los Guabos y Av. El Inca", "0969379333"));

    @Test
    @DisplayName("Las observaciones conservan los saltos de linea que escribio el usuario")
    void observationsKeepLineBreaks() throws IOException {
        String text = text(generator.generate(quotation("Incluye cable y cajas.\nEl descuento es por la inspeccion.")));

        assertThat(text.lines().map(String::strip))
                .contains("Incluye cable y cajas.", "El descuento es por la inspeccion.");
    }

    @Test
    @DisplayName("Unas observaciones largas siguen en otra pagina y no se pierde ninguna linea")
    void longObservationsContinueOnNextPage() throws IOException {
        String observations = IntStream.rangeClosed(1, 90)
                .mapToObj(i -> "Condicion numero " + i)
                .collect(Collectors.joining("\n"));

        byte[] pdf = generator.generate(quotation(observations));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
        }
        String text = text(pdf);
        assertThat(text).contains("Condicion numero 1", "Condicion numero 90", "OBSERVACIONES (continuacion)");
    }

    @Test
    @DisplayName("wrap: cada salto de linea empieza una linea y una linea en blanco se conserva")
    void wrapHonoursNewlines() throws IOException {
        try (PDDocument document = new PDDocument(); PdfCanvas canvas = new PdfCanvas(document)) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            assertThat(canvas.wrap("uno\r\ndos\n\ntres   cuatro", font, 10f, 500f))
                    .containsExactly("uno", "dos", "", "tres cuatro");
            assertThat(canvas.wrap("  \n ", font, 10f, 500f)).isEmpty();
        }
    }

    private Quotation quotation(String observations) {
        Client client = Client.builder()
                .nameOrBusinessName("Consumidor Final").documentNumber("9999999999")
                .phone("0999999999").address("Quito").city("Quito")
                .build();
        QuotationDetail detail = QuotationDetail.builder()
                .descriptionFrozen("Servicio de instalacion").quantity(BigDecimal.ONE)
                .unitPriceFinal(new BigDecimal("20.00")).lineTotal(new BigDecimal("20.00"))
                .build();

        return Quotation.builder()
                .quotationNumber("COT-2026-000001").client(client)
                .issuedAt(LocalDate.of(2026, 9, 30)).validUntil(LocalDate.of(2026, 10, 7))
                .currency("USD").discount(BigDecimal.ZERO).vatValueHistorical(BigDecimal.ZERO)
                .total(new BigDecimal("20.00")).details(List.of(detail)).observations(observations)
                .build();
    }

    private String text(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
