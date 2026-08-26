package com.tidsec.novaeyetech_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Alta de un adjunto ya almacenado por fuera del backend. */
@Data
public class AttachmentRequest {

    @NotBlank
    @Size(max = 80)
    private String sourceEntity;

    @NotBlank
    @Size(max = 80)
    private String sourceEntityId;

    @NotBlank
    @Size(max = 255)
    private String originalName;

    @NotBlank
    @Size(max = 255)
    private String storedName;

    @NotBlank
    @Size(max = 120)
    private String mimeType;

    @NotBlank
    @Size(max = 255)
    private String storagePath;

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long size;
}
