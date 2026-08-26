package com.tidsec.novaeyetech_backend.dto;

import com.tidsec.novaeyetech_backend.model.enums.QuotationStatus;
import jakarta.validation.constraints.NotNull;

public record QuotationStatusRequest(@NotNull QuotationStatus status) {
}
