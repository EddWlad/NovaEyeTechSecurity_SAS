package com.tidsec.novaeyetech_backend.security;

import tools.jackson.databind.ObjectMapper;
import com.tidsec.novaeyetech_backend.exception.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Los errores 401 y 403 los emite la cadena de filtros, antes de que exista un controlador, asi que
 * el {@code @RestControllerAdvice} no los ve. Este componente les da el mismo cuerpo JSON que el
 * resto de la API para que el interceptor del frontend siempre encuentre `message`.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponder implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, "No autorizado. Inicia sesion nuevamente.");
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, "No tienes permisos para esta operacion");
    }

    private void write(HttpServletRequest request,
                       HttpServletResponse response,
                       HttpStatus status,
                       String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiErrorResponse body = ApiErrorResponse.of(
                status.value(), status.getReasonPhrase(), message, request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
