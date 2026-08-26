package com.tidsec.novaeyetech_backend.service;

import com.tidsec.novaeyetech_backend.dto.MaintenanceCommentRequest;
import com.tidsec.novaeyetech_backend.model.MaintenanceComment;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import java.util.List;
import java.util.UUID;

public interface IMaintenanceCommentService {

    MaintenanceComment create(MaintenanceCommentRequest request, AuthenticatedUser actor);

    List<MaintenanceComment> findByMaintenance(UUID maintenanceId);
}
