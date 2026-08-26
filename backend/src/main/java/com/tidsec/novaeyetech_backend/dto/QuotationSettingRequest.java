package com.tidsec.novaeyetech_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * Actualizacion parcial de los parametros de cotizacion. Todo es opcional: lo que no venga conserva
 * su valor actual.
 */
@Data
public class QuotationSettingRequest {

    /** Porcentaje decimal positivo, con o sin parte fraccionaria. */
    private static final String PERCENT_PATTERN = "^\\d+(\\.\\d+)?$";

    @DecimalMin(value = "0.0", message = "El IVA no puede ser negativo")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal currentVat;

    @NotEmpty(message = "Debe existir al menos un porcentaje de IVA permitido")
    private List<@Pattern(regexp = PERCENT_PATTERN, message = "Porcentaje de IVA invalido") String> allowedVatRates;

    @NotEmpty(message = "Debe existir al menos un margen permitido")
    private List<@Pattern(regexp = PERCENT_PATTERN, message = "Margen invalido") String> allowedMargins;

    @DecimalMin(value = "0.0", message = "El margen por defecto no puede ser negativo")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal defaultMargin;

    @Size(max = 8)
    private String defaultCurrency;

    @Min(value = 1, message = "La vigencia debe ser de al menos 1 dia")
    private Integer defaultValidityDays;
}
