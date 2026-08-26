package com.tidsec.novaeyetech_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres") String password
) {
}
