package com.tidsec.novaeyetech_backend.service;

import com.tidsec.novaeyetech_backend.dto.MaintenanceRequest;
import com.tidsec.novaeyetech_backend.model.Maintenance;
import com.tidsec.novaeyetech_backend.model.enums.MaintenanceStatus;
import com.tidsec.novaeyetech_backend.model.enums.MaintenanceType;
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

    /** Pagina filtrada por cliente o sistema ({@code search}), tipo y estado, dentro del alcance del usuario. */
    Page<Maintenance> findAll(AuthenticatedUser actor, String search, MaintenanceType type,
                              MaintenanceStatus status, Pageable pageable);

    /**
     * Mantenimientos que no estan COMPLETADO, dentro del alcance del usuario. Incluye los CANCELADO:
     * es el mismo criterio que aplicaba el dashboard cuando filtraba el listado en el navegador.
     */
    Page<Maintenance> findPending(AuthenticatedUser actor, Pageable pageable);

    Maintenance findById(UUID id, AuthenticatedUser actor);

    Maintenance update(UUID id, MaintenanceRequest request, AuthenticatedUser actor);

    void delete(UUID id, AuthenticatedUser actor);
}
