package com.tidsec.novaeyetech_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class MaintenanceCommentRequest {

    @NotNull(message = "maintenanceId es obligatorio")
    private UUID maintenanceId;

    @NotBlank(message = "El comentario no puede estar vacio")
    private String comment;
}
