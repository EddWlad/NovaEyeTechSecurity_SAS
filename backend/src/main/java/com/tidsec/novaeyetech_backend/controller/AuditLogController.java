package com.tidsec.novaeyetech_backend.controller;

import com.tidsec.novaeyetech_backend.dto.AuditLogDTO;
import com.tidsec.novaeyetech_backend.dto.common.PaginationQuery;
import com.tidsec.novaeyetech_backend.service.IAuditLogService;
import com.tidsec.novaeyetech_backend.util.ListingResponder;
import com.tidsec.novaeyetech_backend.util.PaginationSupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consulta de la bitacora. Solo lectura y solo para ADMINISTRADOR. */
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private static final int UNPAGINATED_LIMIT = 100;

    private final IAuditLogService service;
    private final ListingResponder listingResponder;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Object> findAll(@Valid PaginationQuery query) {
        int limit = query.getLimit() != null ? query.getLimit() : UNPAGINATED_LIMIT;

        return listingResponder.respond(query, PaginationSupport.AUDIT_DEFAULT_LIMIT,
                () -> service.findRecent(limit), service::findAll, AuditLogDTO.class);
    }
}
