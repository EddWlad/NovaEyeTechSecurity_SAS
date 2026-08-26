package com.tidsec.novaeyetech_backend.controller;

import com.tidsec.novaeyetech_backend.dto.CategoryDTO;
import com.tidsec.novaeyetech_backend.dto.CategoryRequest;
import com.tidsec.novaeyetech_backend.dto.common.MessageResponse;
import com.tidsec.novaeyetech_backend.dto.common.PaginationQuery;
import com.tidsec.novaeyetech_backend.dto.validation.OnCreate;
import com.tidsec.novaeyetech_backend.service.IServiceCategoryService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import com.tidsec.novaeyetech_backend.util.ListingResponder;
import com.tidsec.novaeyetech_backend.util.PaginationSupport;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/service-categories")
@RequiredArgsConstructor
public class ServiceCategoryController {

    private final IServiceCategoryService service;
    private final ListingResponder listingResponder;
    private final DtoMapper dtoMapper;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CategoryDTO> create(@Validated(OnCreate.class) @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(dtoMapper.map(service.create(request), CategoryDTO.class));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<Object> findAll(@Valid PaginationQuery query) {
        return listingResponder.respond(query, PaginationSupport.DEFAULT_LIMIT,
                service::findAll, service::findAll, CategoryDTO.class);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<CategoryDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(dtoMapper.map(service.findById(id), CategoryDTO.class));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CategoryDTO> update(@PathVariable UUID id,
                                              @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(dtoMapper.map(service.update(id, request), CategoryDTO.class));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id) {
        service.delete(id);

        return ResponseEntity.ok(new MessageResponse("Categoria eliminada correctamente"));
    }
}
