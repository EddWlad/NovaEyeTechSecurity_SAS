package com.tidsec.novaeyetech_backend.repo;

import com.tidsec.novaeyetech_backend.model.MaintenanceComment;
import java.util.List;
import java.util.UUID;

public interface IMaintenanceCommentRepo extends IGenericRepo<MaintenanceComment, UUID> {

    List<MaintenanceComment> findByMaintenance_IdOrderByCreatedAtAsc(UUID maintenanceId);
}
