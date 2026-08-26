package com.tidsec.novaeyetech_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tidsec.novaeyetech_backend.model.enums.QuotationItemType;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

/** Linea congelada de la cotizacion, tal como quedo al emitirse. */
@Data
public class QuotationDetailDTO {

    private UUID id;
    private Integer lineNumber;
    private QuotationItemType itemType;
    private String referenceId;
    private String descriptionFrozen;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal quantity;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal basePriceHistorical;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal vatPercentHistorical;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal marginPercentHistorical;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal unitPriceFinal;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal lineSubtotalBase;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal lineVatValue;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal lineTotal;
}
