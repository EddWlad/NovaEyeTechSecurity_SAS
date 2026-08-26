package com.tidsec.novaeyetech_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tidsec.novaeyetech_backend.model.enums.QuotationItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

/**
 * Item solicitado para una cotizacion.
 *
 * <p>vatPercent y marginPercent son opcionales: si no vienen se toman los valores vigentes en la
 * configuracion. El margen 0 es un valor legitimo y no debe rechazarse.
 */
@Data
public class QuotationItemRequest {

    @NotNull(message = "itemType es obligatorio")
    private QuotationItemType itemType;

    private UUID productId;

    private UUID serviceId;

    /** Descripcion a congelar. Si no viene se usa la del producto o servicio de origen. */
    private String description;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal quantity;

    @DecimalMin(value = "0.0", message = "El IVA no puede ser negativo")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal vatPercent;

    @DecimalMin(value = "0.0", message = "El margen no puede ser negativo")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal marginPercent;
}
