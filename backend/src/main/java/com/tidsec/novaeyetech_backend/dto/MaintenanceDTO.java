package com.tidsec.novaeyetech_backend.dto;

import com.tidsec.novaeyetech_backend.model.enums.MaintenanceStatus;
import com.tidsec.novaeyetech_backend.model.enums.MaintenanceType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class MaintenanceDTO {

    private UUID id;
    private ClientDTO client;
    private MaintenanceType type;
    private MaintenanceStatus status;
    private LocalDate scheduledDate;
    private LocalDate executionDate;
    private UserDTO technician;
    private String intervenedSystem;
    private String diagnosis;
    private String appliedSolution;
    private String observations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
