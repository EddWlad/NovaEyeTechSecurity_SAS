package com.tidsec.novaeyetech_backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class MaintenanceCommentDTO {

    private UUID id;
    private MaintenanceDTO maintenance;
    private UserDTO user;
    private String comment;
    private LocalDateTime createdAt;
}
