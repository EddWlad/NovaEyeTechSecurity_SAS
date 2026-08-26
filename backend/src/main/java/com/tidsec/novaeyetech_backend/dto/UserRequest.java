package com.tidsec.novaeyetech_backend.dto;

import com.tidsec.novaeyetech_backend.dto.validation.OnCreate;
import com.tidsec.novaeyetech_backend.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {

    @NotBlank(groups = OnCreate.class)
    @Email
    @Size(max = 180)
    private String email;

    @NotBlank(groups = OnCreate.class)
    @Size(min = 3, max = 180)
    private String fullName;

    @NotBlank(groups = OnCreate.class)
    @Size(min = 6, max = 72, message = "La contrasena debe tener al menos 6 caracteres")
    private String password;

    @NotNull(groups = OnCreate.class)
    private Role role;

    @Size(max = 30)
    private String phone;

    private String avatarDataUrl;

    private Boolean active;
}
