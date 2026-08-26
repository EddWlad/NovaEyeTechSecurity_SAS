package com.tidsec.novaeyetech_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tidsec.novaeyetech_backend.model.enums.QuotationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class QuotationDTO {

    private UUID id;
    private String quotationNumber;
    private ClientDTO client;
    private UserDTO createdByUser;
    private LocalDate issuedAt;
    private LocalDate validUntil;
    private QuotationStatus status;
    private String observations;

    /** Suma de bases sin IVA. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal subtotal;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal discount;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal vatPercentHistorical;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal vatValueHistorical;

    /** Total bruto menos descuento. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal total;

    private String currency;
    private List<QuotationDetailDTO> details;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
