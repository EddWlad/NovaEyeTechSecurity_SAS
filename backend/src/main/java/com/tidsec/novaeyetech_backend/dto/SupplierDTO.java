package com.tidsec.novaeyetech_backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class SupplierDTO {

    private UUID id;
    private String businessName;
    private String ruc;
    private String contact;
    private String phone;
    private String email;
    private String address;
    private String city;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
