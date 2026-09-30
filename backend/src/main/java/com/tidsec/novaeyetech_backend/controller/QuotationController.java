package com.tidsec.novaeyetech_backend.controller;

import com.tidsec.novaeyetech_backend.dto.PdfPreviewDTO;
import com.tidsec.novaeyetech_backend.dto.QuotationDTO;
import com.tidsec.novaeyetech_backend.dto.QuotationRequest;
import com.tidsec.novaeyetech_backend.dto.QuotationStatusRequest;
import com.tidsec.novaeyetech_backend.dto.common.PageResponse;
import com.tidsec.novaeyetech_backend.dto.common.PaginationQuery;
import com.tidsec.novaeyetech_backend.model.Quotation;
import com.tidsec.novaeyetech_backend.model.enums.QuotationStatus;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IQuotationService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import com.tidsec.novaeyetech_backend.util.PaginationSupport;
import jakarta.validation.Valid;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cotizaciones.
 *
 * <p>El listado no usa {@code ListingResponder} porque el alcance depende del usuario autenticado y
 * las consultas necesitan recibirlo; el contrato de doble modo es el mismo.
 */
@RestController
@RequestMapping("/quotations")
@RequiredArgsConstructor
public class QuotationController {

    private final IQuotationService service;
    private final DtoMapper dtoMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_OPERATIVO', 'TECNICO')")
    public ResponseEntity<QuotationDTO> create(@Valid @RequestBody QuotationRequest request,
                                               @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(dtoMapper.map(service.create(request, actor), QuotationDTO.class));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_OPERATIVO', 'TECNICO')")
    public ResponseEntity<Object> findAll(@AuthenticationPrincipal AuthenticatedUser actor,
                                          @Valid PaginationQuery query,
                                          @RequestParam(required = false) QuotationStatus status) {
        if (!query.isPaginated()) {
            return ResponseEntity.ok(dtoMapper.mapList(service.findAll(actor), QuotationDTO.class));
        }

        Page<Quotation> page = service.findAll(actor, query.getSearch(), status,
                PaginationSupport.toPageable(query, PaginationSupport.DEFAULT_LIMIT));

        return ResponseEntity.ok(
                PageResponse.from(page, dtoMapper.mapList(page.getContent(), QuotationDTO.class)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_OPERATIVO', 'TECNICO')")
    public ResponseEntity<QuotationDTO> findById(@PathVariable UUID id,
                                                  @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(dtoMapper.map(service.findById(id, actor), QuotationDTO.class));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_OPERATIVO', 'TECNICO')")
    public ResponseEntity<QuotationDTO> updateDraft(@PathVariable UUID id,
                                                     @Valid @RequestBody QuotationRequest request,
                                                     @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(dtoMapper.map(service.updateDraft(id, request, actor), QuotationDTO.class));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_OPERATIVO', 'TECNICO')")
    public ResponseEntity<QuotationDTO> updateStatus(@PathVariable UUID id,
                                                      @Valid @RequestBody QuotationStatusRequest request,
                                                      @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(dtoMapper.map(service.updateStatus(id, request, actor), QuotationDTO.class));
    }

    /** Se sirve inline para que el frontend pueda mostrarlo en su vista previa. */
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_OPERATIVO', 'TECNICO')")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id,
                                              @AuthenticationPrincipal AuthenticatedUser actor) {
        byte[] pdf = service.buildPdf(id, actor);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"cotizacion-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * Vista previa para celulares: los navegadores moviles no dibujan un PDF embebido. Mismo alcance por
     * rol que el PDF: un tecnico solo ve la de sus propias cotizaciones.
     */
    @GetMapping("/{id}/pdf/preview")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_OPERATIVO', 'TECNICO')")
    public ResponseEntity<PdfPreviewDTO> previewPdf(@PathVariable UUID id,
                                                    @AuthenticationPrincipal AuthenticatedUser actor) {
        Base64.Encoder encoder = Base64.getEncoder();

        return ResponseEntity.ok(new PdfPreviewDTO(service.buildPdfPreview(id, actor).stream()
                .map(png -> "data:image/png;base64," + encoder.encodeToString(png))
                .toList()));
    }
}
