package com.tidsec.novaeyetech_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tidsec.novaeyetech_backend.dto.validation.OnCreate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class ServiceRequest {

    @NotNull(groups = OnCreate.class)
    private UUID categoryId;

    @NotBlank(groups = OnCreate.class)
    @Size(max = 180)
    private String name;

    @NotBlank(groups = OnCreate.class)
    private String description;

    @NotNull(groups = OnCreate.class)
    @DecimalMin(value = "0.0", message = "El costo base no puede ser negativo")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal baseCost;

    private Boolean active;
}
