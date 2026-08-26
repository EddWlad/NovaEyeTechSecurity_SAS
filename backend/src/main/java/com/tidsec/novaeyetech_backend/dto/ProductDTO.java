package com.tidsec.novaeyetech_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

/**
 * Producto expuesto por la API.
 *
 * <p>Los importes se serializan como String para conservar la escala de dos decimales que el
 * frontend ya recibia del backend original (columnas numeric de TypeORM).
 */
@Data
public class ProductDTO {

    private UUID id;
    private CategoryDTO category;
    private UUID categoryId;
    private SupplierDTO mainSupplier;
    private UUID mainSupplierId;
    private String internalCode;
    private String name;
    private String brand;
    private String model;
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal baseCost;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal stock;

    private String unit;
    private String imageUrl;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
