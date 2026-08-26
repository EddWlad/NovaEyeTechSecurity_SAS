package com.tidsec.novaeyetech_backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class AuditLogDTO {

    private UUID id;
    private String module;
    private String entity;
    private String entityId;
    private String action;
    private String user;
    private String summary;
    private String payloadSummary;
    private LocalDateTime createdAt;
}
