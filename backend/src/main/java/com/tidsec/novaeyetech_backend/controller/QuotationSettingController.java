package com.tidsec.novaeyetech_backend.controller;

import com.tidsec.novaeyetech_backend.dto.QuotationSettingDTO;
import com.tidsec.novaeyetech_backend.dto.QuotationSettingRequest;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IQuotationSettingService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Parametros del motor de cotizaciones: los lee cualquier usuario, solo ADMINISTRADOR los cambia. */
@RestController
@RequestMapping("/quotation-settings")
@RequiredArgsConstructor
public class QuotationSettingController {

    private final IQuotationSettingService service;
    private final DtoMapper dtoMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<QuotationSettingDTO> getSettings() {
        return ResponseEntity.ok(dtoMapper.map(service.getCurrentSettings(), QuotationSettingDTO.class));
    }

    @PatchMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<QuotationSettingDTO> updateSettings(
            @Valid @RequestBody QuotationSettingRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor) {

        return ResponseEntity.ok(dtoMapper.map(service.updateSettings(request, actor), QuotationSettingDTO.class));
    }
}
