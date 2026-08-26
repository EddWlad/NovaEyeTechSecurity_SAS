package com.tidsec.novaeyetech_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class AttachmentDTO {

    private UUID id;
    private String sourceEntity;
    private String sourceEntityId;
    private String originalName;
    private String storedName;
    private String mimeType;

    /** URL segura del recurso. El frontend puede usarla directo en un `img` sin pasar por el backend. */
    private String storagePath;

    /** El tamano viaja como String, igual que la columna bigint del backend original. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long size;

    private String uploadedBy;
    private LocalDateTime createdAt;
}
