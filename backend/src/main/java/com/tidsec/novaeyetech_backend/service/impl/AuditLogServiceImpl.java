package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.dto.common.AuditEntry;
import com.tidsec.novaeyetech_backend.model.AuditLog;
import com.tidsec.novaeyetech_backend.repo.IAuditLogRepo;
import com.tidsec.novaeyetech_backend.service.IAuditLogService;
import com.tidsec.novaeyetech_backend.util.PaginationSupport;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements IAuditLogService {

    private static final int SUMMARY_MAX_LENGTH = 300;

    private final IAuditLogRepo repo;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void register(AuditEntry entry) {
        AuditLog log = AuditLog.builder()
                .module(entry.module())
                .entity(entry.entity())
                .entityId(entry.entityId())
                .action(entry.action())
                .user(entry.user())
                .summary(truncate(entry.summary()))
                .payloadSummary(serializePayload(entry.payload()))
                .build();

        repo.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> findRecent(int limit) {
        return repo.findAll(PageRequest.of(0, PaginationSupport.clampLimit(limit), PaginationSupport.newestFirst()))
                .getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    /** Un resumen mal formado no debe tumbar la operacion de negocio que lo origino. */
    private String serializePayload(Object payload) {
        if (payload == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            log.warn("No se pudo serializar el resumen de auditoria", ex);
            return null;
        }
    }

    private String truncate(String summary) {
        return summary != null && summary.length() > SUMMARY_MAX_LENGTH
                ? summary.substring(0, SUMMARY_MAX_LENGTH)
                : summary;
    }
}
