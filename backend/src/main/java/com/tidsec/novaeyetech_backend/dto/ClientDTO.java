package com.tidsec.novaeyetech_backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class ClientDTO {

    private UUID id;
    private String nameOrBusinessName;
    private String documentNumber;
    private String phone;
    private String email;
    private String address;
    private String city;
    private String commercialReference;
    private String observations;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
