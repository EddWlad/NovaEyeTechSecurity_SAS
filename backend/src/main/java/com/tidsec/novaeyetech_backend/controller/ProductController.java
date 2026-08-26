package com.tidsec.novaeyetech_backend.controller;

import com.tidsec.novaeyetech_backend.dto.ProductDTO;
import com.tidsec.novaeyetech_backend.dto.ProductRequest;
import com.tidsec.novaeyetech_backend.dto.common.MessageResponse;
import com.tidsec.novaeyetech_backend.dto.common.PaginationQuery;
import com.tidsec.novaeyetech_backend.dto.validation.OnCreate;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IProductService;
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
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final IProductService service;
    private final ListingResponder listingResponder;
    private final DtoMapper dtoMapper;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProductDTO> create(@Validated(OnCreate.class) @RequestBody ProductRequest request,
                                             @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(dtoMapper.map(service.create(request, actor), ProductDTO.class));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<Object> findAll(@Valid PaginationQuery query) {
        return listingResponder.respond(query, PaginationSupport.DEFAULT_LIMIT,
                service::findAll, service::findAll, ProductDTO.class);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<ProductDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(dtoMapper.map(service.findById(id), ProductDTO.class));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ProductDTO> update(@PathVariable UUID id,
                                             @Valid @RequestBody ProductRequest request,
                                             @AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(dtoMapper.map(service.update(id, request, actor), ProductDTO.class));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id,
                                                  @AuthenticationPrincipal AuthenticatedUser actor) {
        service.delete(id, actor);

        return ResponseEntity.ok(new MessageResponse("Producto eliminado correctamente"));
    }
}
