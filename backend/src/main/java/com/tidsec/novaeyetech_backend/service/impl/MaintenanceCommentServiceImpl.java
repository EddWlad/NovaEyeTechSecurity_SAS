package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.dto.MaintenanceCommentRequest;
import com.tidsec.novaeyetech_backend.dto.common.AuditEntry;
import com.tidsec.novaeyetech_backend.exception.ResourceNotFoundException;
import com.tidsec.novaeyetech_backend.model.Maintenance;
import com.tidsec.novaeyetech_backend.model.MaintenanceComment;
import com.tidsec.novaeyetech_backend.model.User;
import com.tidsec.novaeyetech_backend.repo.IMaintenanceCommentRepo;
import com.tidsec.novaeyetech_backend.repo.IMaintenanceRepo;
import com.tidsec.novaeyetech_backend.repo.IUserRepo;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IAuditLogService;
import com.tidsec.novaeyetech_backend.service.IMaintenanceCommentService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MaintenanceCommentServiceImpl implements IMaintenanceCommentService {

    private static final String MODULE = "maintenance-comments";
    private static final String ENTITY = "MaintenanceComment";

    private final IMaintenanceCommentRepo repo;
    private final IMaintenanceRepo maintenanceRepo;
    private final IUserRepo userRepo;
    private final IAuditLogService auditLogService;

    @Override
    @Transactional
    public MaintenanceComment create(MaintenanceCommentRequest request, AuthenticatedUser actor) {
        Maintenance maintenance = maintenanceRepo.findById(request.getMaintenanceId())
                .orElseThrow(() -> new ResourceNotFoundException("Mantenimiento no encontrado"));
        User author = userRepo.findById(actor.id())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        MaintenanceComment comment = MaintenanceComment.builder()
                .maintenance(maintenance)
                .user(author)
                .comment(request.getComment())
                .build();

        MaintenanceComment saved = repo.save(comment);
        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(saved.getId().toString())
                .action(AuditEntry.ACTION_CREATE)
                .user(actor.email())
                .summary("Comentario tecnico agregado al mantenimiento " + maintenance.getId())
                .payload(request)
                .build());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceComment> findByMaintenance(UUID maintenanceId) {
        return repo.findByMaintenance_IdOrderByCreatedAtAsc(maintenanceId);
    }
}
