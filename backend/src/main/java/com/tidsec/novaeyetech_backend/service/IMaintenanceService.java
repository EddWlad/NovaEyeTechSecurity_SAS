package com.tidsec.novaeyetech_backend.service;

import com.tidsec.novaeyetech_backend.dto.MaintenanceRequest;
import com.tidsec.novaeyetech_backend.model.Maintenance;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Un TECNICO solo ve y edita los mantenimientos donde figura como tecnico asignado. */
public interface IMaintenanceService {

    Maintenance create(MaintenanceRequest request, AuthenticatedUser actor);

    List<Maintenance> findAll(AuthenticatedUser actor);

    Page<Maintenance> findAll(AuthenticatedUser actor, Pageable pageable);

    Maintenance findById(UUID id, AuthenticatedUser actor);

    Maintenance update(UUID id, MaintenanceRequest request, AuthenticatedUser actor);

    void delete(UUID id, AuthenticatedUser actor);
}
