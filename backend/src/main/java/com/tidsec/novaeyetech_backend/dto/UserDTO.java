package com.tidsec.novaeyetech_backend.dto;

import com.tidsec.novaeyetech_backend.model.enums.Role;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

/** Usuario expuesto por la API. No incluye el hash de contrasena por diseno. */
@Data
public class UserDTO {

    private UUID id;
    private String email;
    private String fullName;
    private Role role;
    private String phone;
    private String avatarDataUrl;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
