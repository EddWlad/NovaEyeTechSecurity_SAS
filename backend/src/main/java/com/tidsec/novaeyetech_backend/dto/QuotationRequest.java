package com.tidsec.novaeyetech_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tidsec.novaeyetech_backend.model.enums.QuotationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class QuotationRequest {

    @NotNull(message = "clientId es obligatorio")
    private UUID clientId;

    private LocalDate issuedAt;

    private LocalDate validUntil;

    private QuotationStatus status;

    private String observations;

    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal discount;

    @Size(max = 8)
    private String currency;

    @Valid
    @NotEmpty(message = "La cotizacion debe tener al menos un item")
    private List<QuotationItemRequest> items;
}
