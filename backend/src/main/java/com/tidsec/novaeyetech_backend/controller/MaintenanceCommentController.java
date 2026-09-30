package com.tidsec.novaeyetech_backend.controller;

import com.tidsec.novaeyetech_backend.dto.MaintenanceCommentDTO;
import com.tidsec.novaeyetech_backend.dto.MaintenanceCommentRequest;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IMaintenanceCommentService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/maintenance-comments")
@RequiredArgsConstructor
public class MaintenanceCommentController {

    private final IMaintenanceCommentService service;
    private final DtoMapper dtoMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_OPERATIVO', 'TECNICO')")
    public ResponseEntity<MaintenanceCommentDTO> create(@Valid @RequestBody MaintenanceCommentRequest request,
                                                        @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(dtoMapper.map(service.create(request, actor), MaintenanceCommentDTO.class));
    }

    @GetMapping("/maintenance/{maintenanceId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_OPERATIVO', 'TECNICO')")
    public ResponseEntity<List<MaintenanceCommentDTO>> findByMaintenance(@PathVariable UUID maintenanceId) {
        return ResponseEntity.ok(
                dtoMapper.mapList(service.findByMaintenance(maintenanceId), MaintenanceCommentDTO.class));
    }
}
