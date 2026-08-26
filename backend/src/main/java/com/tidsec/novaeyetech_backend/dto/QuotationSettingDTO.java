package com.tidsec.novaeyetech_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class QuotationSettingDTO {

    private UUID id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal currentVat;

    private List<String> allowedVatRates;
    private List<String> allowedMargins;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal defaultMargin;

    private String defaultCurrency;
    private Integer defaultValidityDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
