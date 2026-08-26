package com.tidsec.novaeyetech_backend.dto;

import com.tidsec.novaeyetech_backend.dto.validation.OnCreate;
import com.tidsec.novaeyetech_backend.model.enums.MaintenanceStatus;
import com.tidsec.novaeyetech_backend.model.enums.MaintenanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class MaintenanceRequest {

    @NotNull(groups = OnCreate.class, message = "clientId es obligatorio")
    private UUID clientId;

    @NotNull(groups = OnCreate.class, message = "El tipo de mantenimiento es obligatorio")
    private MaintenanceType type;

    private MaintenanceStatus status;

    @NotNull(groups = OnCreate.class, message = "La fecha programada es obligatoria")
    private LocalDate scheduledDate;

    private LocalDate executionDate;

    /** Si no viene, el mantenimiento se asigna al usuario que lo crea. */
    private UUID technicianId;

    @NotBlank(groups = OnCreate.class)
    @Size(max = 255)
    private String intervenedSystem;

    @NotBlank(groups = OnCreate.class)
    private String diagnosis;

    @NotBlank(groups = OnCreate.class)
    private String appliedSolution;

    private String observations;
}
