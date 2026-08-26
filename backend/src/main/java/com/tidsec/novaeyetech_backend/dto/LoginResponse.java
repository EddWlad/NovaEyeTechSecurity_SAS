package com.tidsec.novaeyetech_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Respuesta del login. El campo se serializa como {@code access_token} porque asi lo lee el
 * frontend Angular ({@code AuthService.setSession}).
 */
public record LoginResponse(
        @JsonProperty("access_token") String accessToken,
        UserDTO user
) {
}
