package com.tidsec.novaeyetech_backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

/** Categoria de catalogo. La misma forma sirve para productos y para servicios. */
@Data
public class CategoryDTO {

    private UUID id;
    private String name;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
