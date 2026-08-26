package com.tidsec.novaeyetech_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class ServiceDTO {

    private UUID id;
    private CategoryDTO category;
    private UUID categoryId;
    private String name;
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal baseCost;

    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
