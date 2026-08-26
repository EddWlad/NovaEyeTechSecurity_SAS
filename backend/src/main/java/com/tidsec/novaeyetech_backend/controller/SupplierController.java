package com.tidsec.novaeyetech_backend.controller;

import com.tidsec.novaeyetech_backend.dto.SupplierDTO;
import com.tidsec.novaeyetech_backend.dto.SupplierRequest;
import com.tidsec.novaeyetech_backend.dto.common.MessageResponse;
import com.tidsec.novaeyetech_backend.dto.common.PaginationQuery;
import com.tidsec.novaeyetech_backend.dto.validation.OnCreate;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.ISupplierService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import com.tidsec.novaeyetech_backend.util.ListingResponder;
import com.tidsec.novaeyetech_backend.util.PaginationSupport;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final ISupplierService service;
    private final ListingResponder listingResponder;
    private final DtoMapper dtoMapper;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SupplierDTO> create(@Validated(OnCreate.class) @RequestBody SupplierRequest request,
                                              @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(dtoMapper.map(service.create(request, actor), SupplierDTO.class));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<Object> findAll(@Valid PaginationQuery query) {
        return listingResponder.respond(query, PaginationSupport.DEFAULT_LIMIT,
                service::findAll, service::findAll, SupplierDTO.class);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<SupplierDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(dtoMapper.map(service.findById(id), SupplierDTO.class));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SupplierDTO> update(@PathVariable UUID id,
                                              @Valid @RequestBody SupplierRequest request,
                                              @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(dtoMapper.map(service.update(id, request, actor), SupplierDTO.class));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id,
                                                  @AuthenticationPrincipal AuthenticatedUser actor) {
        service.delete(id, actor);

        return ResponseEntity.ok(new MessageResponse("Proveedor eliminado correctamente"));
    }
}
