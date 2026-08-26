package com.tidsec.novaeyetech_backend.controller;

import com.tidsec.novaeyetech_backend.dto.MaintenanceDTO;
import com.tidsec.novaeyetech_backend.dto.MaintenanceRequest;
import com.tidsec.novaeyetech_backend.dto.common.MessageResponse;
import com.tidsec.novaeyetech_backend.dto.common.PageResponse;
import com.tidsec.novaeyetech_backend.dto.common.PaginationQuery;
import com.tidsec.novaeyetech_backend.dto.validation.OnCreate;
import com.tidsec.novaeyetech_backend.model.Maintenance;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IMaintenanceService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import com.tidsec.novaeyetech_backend.util.PaginationSupport;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final IMaintenanceService service;
    private final DtoMapper dtoMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<MaintenanceDTO> create(
            @Validated(OnCreate.class) @RequestBody MaintenanceRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor) {

        return ResponseEntity.ok(dtoMapper.map(service.create(request, actor), MaintenanceDTO.class));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<Object> findAll(@AuthenticationPrincipal AuthenticatedUser actor,
                                          @Valid PaginationQuery query) {
        if (!query.isPaginated()) {
            return ResponseEntity.ok(dtoMapper.mapList(service.findAll(actor), MaintenanceDTO.class));
        }

        Page<Maintenance> page = service.findAll(actor,
                PaginationSupport.toPageable(query, PaginationSupport.DEFAULT_LIMIT));

        return ResponseEntity.ok(
                PageResponse.from(page, dtoMapper.mapList(page.getContent(), MaintenanceDTO.class)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<MaintenanceDTO> findById(@PathVariable UUID id,
                                                    @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(dtoMapper.map(service.findById(id, actor), MaintenanceDTO.class));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<MaintenanceDTO> update(@PathVariable UUID id,
                                                  @Valid @RequestBody MaintenanceRequest request,
                                                  @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(dtoMapper.map(service.update(id, request, actor), MaintenanceDTO.class));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id,
                                                   @AuthenticationPrincipal AuthenticatedUser actor) {
        service.delete(id, actor);

        return ResponseEntity.ok(new MessageResponse("Mantenimiento eliminado correctamente"));
    }
}
