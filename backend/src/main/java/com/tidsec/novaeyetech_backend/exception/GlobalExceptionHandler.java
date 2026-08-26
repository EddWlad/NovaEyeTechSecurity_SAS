package com.tidsec.novaeyetech_backend.exception;

import tools.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce excepciones a {@link ApiErrorResponse}.
 *
 * <p>Equivalencias con el backend NestJS original:
 * <ul>
 *   <li>{@code NotFoundException} -> {@link ResourceNotFoundException} (404)</li>
 *   <li>{@code BadRequestException} -> {@link BusinessRuleException} / {@link DuplicateResourceException} (400)</li>
 *   <li>{@code ValidationPipe(forbidNonWhitelisted)} -> {@link UnrecognizedPropertyException} (400)</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({BusinessRuleException.class, DuplicateResourceException.class, RelatedRecordsException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** Unicidad que se escapa a la validacion previa y estalla en la base. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "La operacion viola una restriccion de integridad de datos.", request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                     WebRequest request) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                String.join(", ", details), path(request), details));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex, WebRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciales invalidas", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, "No tienes permisos para esta operacion", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, WebRequest request) {
        logger.error("Error no controlado en " + path(request), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado en el servidor.", request);
    }

    /** Validacion de {@code @Valid}: un solo mensaje plano mas el detalle por campo. */
    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                String.join(", ", details), path(request), details));
    }

    /**
     * Cuerpo ilegible. Cubre el equivalente de {@code forbidNonWhitelisted}: un campo no declarado
     * en el DTO llega como {@link UnrecognizedPropertyException} y debe responder 400, no 500.
     */
    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        String message = ex.getCause() instanceof UnrecognizedPropertyException unrecognized
                ? "Campo no permitido en la peticion: " + unrecognized.getPropertyName()
                : "El cuerpo de la peticion no es valido.";

        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), message, path(request)));
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, WebRequest request) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(status.value(), status.getReasonPhrase(), message, path(request)));
    }

    private String path(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : request.getDescription(false);
    }
}
