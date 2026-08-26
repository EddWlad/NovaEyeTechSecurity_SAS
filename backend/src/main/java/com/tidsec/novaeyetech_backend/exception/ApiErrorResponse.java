package com.tidsec.novaeyetech_backend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cuerpo unico de error de la API.
 * {@code message} se mantiene como String porque el interceptor del frontend lee `error.message`.
 * {@code errors} lleva el detalle campo a campo cuando falla la validacion.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int statusCode,
        String error,
        String message,
        String path,
        List<String> errors
) {

    public static ApiErrorResponse of(int statusCode, String error, String message, String path) {
        return new ApiErrorResponse(LocalDateTime.now(), statusCode, error, message, path, null);
    }

    public static ApiErrorResponse of(int statusCode, String error, String message, String path, List<String> errors) {
        return new ApiErrorResponse(LocalDateTime.now(), statusCode, error, message, path, errors);
    }
}
