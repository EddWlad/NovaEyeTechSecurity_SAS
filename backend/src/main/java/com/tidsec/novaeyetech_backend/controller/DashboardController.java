package com.tidsec.novaeyetech_backend.controller;

import com.tidsec.novaeyetech_backend.dto.DashboardSummaryDTO;
import com.tidsec.novaeyetech_backend.dto.MaintenanceDTO;
import com.tidsec.novaeyetech_backend.dto.QuotationDTO;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IDashboardService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService service;
    private final DtoMapper dtoMapper;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_OPERATIVO', 'TECNICO')")
    public ResponseEntity<DashboardSummaryDTO> summary(@AuthenticationPrincipal AuthenticatedUser actor) {
        IDashboardService.DashboardSummary summary = service.summary(actor);

        DashboardSummaryDTO dto = new DashboardSummaryDTO();
        dto.setClients(summary.clients());
        dto.setSuppliers(summary.suppliers());
        dto.setProducts(summary.products());
        dto.setServices(summary.services());
        dto.setPendingMaintenance(summary.pendingMaintenance());
        dto.setQuotations(dtoMapper.mapList(summary.recentQuotations(), QuotationDTO.class));
        dto.setMaintenance(dtoMapper.mapList(summary.pendingMaintenanceItems(), MaintenanceDTO.class));

        return ResponseEntity.ok(dto);
    }
}
