package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.dto.MaintenanceRequest;
import com.tidsec.novaeyetech_backend.dto.common.AuditEntry;
import com.tidsec.novaeyetech_backend.exception.RelatedRecordsException;
import com.tidsec.novaeyetech_backend.exception.ResourceNotFoundException;
import com.tidsec.novaeyetech_backend.model.Client;
import com.tidsec.novaeyetech_backend.model.Maintenance;
import com.tidsec.novaeyetech_backend.model.User;
import com.tidsec.novaeyetech_backend.model.enums.MaintenanceStatus;
import com.tidsec.novaeyetech_backend.model.enums.MaintenanceType;
import com.tidsec.novaeyetech_backend.repo.IClientRepo;
import com.tidsec.novaeyetech_backend.repo.IMaintenanceRepo;
import com.tidsec.novaeyetech_backend.repo.IUserRepo;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IAuditLogService;
import com.tidsec.novaeyetech_backend.service.IMaintenanceService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import com.tidsec.novaeyetech_backend.util.PaginationSupport;
import com.tidsec.novaeyetech_backend.util.SearchSpecification;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mantenimientos.
 *
 * <p>Alcance por rol: un TECNICO solo alcanza los mantenimientos donde figura como tecnico asignado,
 * y un id ajeno responde 404 para no revelar la existencia del registro.
 */
@Service
@RequiredArgsConstructor
public class MaintenanceServiceImpl implements IMaintenanceService {

    private static final String MODULE = "maintenance";
    private static final String ENTITY = "Maintenance";
    private static final String NOT_FOUND = "Mantenimiento no encontrado";

    private final IMaintenanceRepo repo;
    private final IClientRepo clientRepo;
    private final IUserRepo userRepo;
    private final IAuditLogService auditLogService;
    private final DtoMapper dtoMapper;

    @Override
    @Transactional
    public Maintenance create(MaintenanceRequest request, AuthenticatedUser actor) {
        // Sin tecnico explicito, el mantenimiento queda a nombre de quien lo crea.
        UUID technicianId = request.getTechnicianId() != null ? request.getTechnicianId() : actor.id();

        Maintenance maintenance = dtoMapper.map(request, Maintenance.class);
        maintenance.setClient(resolveClient(request.getClientId()));
        maintenance.setTechnician(resolveTechnician(technicianId));
        maintenance.setStatus(request.getStatus() != null ? request.getStatus() : MaintenanceStatus.PENDIENTE);

        Maintenance saved = repo.save(maintenance);
        registerAudit(saved, AuditEntry.ACTION_CREATE, actor,
                "Mantenimiento creado (" + saved.getType() + ")", request);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Maintenance> findAll(AuthenticatedUser actor) {
        return actor.isTechnician()
                ? repo.findByTechnician_Id(actor.id(), PaginationSupport.newestFirst())
                : repo.findAll(PaginationSupport.newestFirst());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Maintenance> findAll(AuthenticatedUser actor, Pageable pageable) {
        return actor.isTechnician()
                ? repo.findByTechnician_Id(actor.id(), pageable)
                : repo.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Maintenance> findAll(AuthenticatedUser actor, String search, MaintenanceType type,
                                     MaintenanceStatus status, Pageable pageable) {
        String term = SearchSpecification.normalize(search);
        if (term == null && type == null && status == null) {
            return findAll(actor, pageable);
        }

        // El alcance por rol va siempre: un filtro nunca amplia lo que un tecnico puede ver.
        Specification<Maintenance> spec = actor.isTechnician()
                ? (root, query, cb) -> cb.equal(root.get("technician").get("id"), actor.id())
                : Specification.unrestricted();
        if (term != null) {
            spec = spec.and(SearchSpecification.containsAny(term, List.of("client.nameOrBusinessName", "intervenedSystem")));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return repo.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Maintenance> findPending(AuthenticatedUser actor, Pageable pageable) {
        return actor.isTechnician()
                ? repo.findByTechnician_IdAndStatusNot(actor.id(), MaintenanceStatus.COMPLETADO, pageable)
                : repo.findByStatusNot(MaintenanceStatus.COMPLETADO, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Maintenance findById(UUID id, AuthenticatedUser actor) {
        return actor.isTechnician()
                ? repo.findByIdAndTechnician_Id(id, actor.id())
                        .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND))
                : repo.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    @Override
    @Transactional
    public Maintenance update(UUID id, MaintenanceRequest request, AuthenticatedUser actor) {
        Maintenance maintenance = findById(id, actor);

        dtoMapper.patch(request, maintenance);

        if (request.getClientId() != null) {
            maintenance.setClient(resolveClient(request.getClientId()));
        }
        if (request.getTechnicianId() != null) {
            maintenance.setTechnician(resolveTechnician(request.getTechnicianId()));
        }

        Maintenance saved = repo.save(maintenance);
        registerAudit(saved, AuditEntry.ACTION_UPDATE, actor,
                "Mantenimiento actualizado (" + saved.getType() + ")", request);

        return saved;
    }

    @Override
    @Transactional
    public void delete(UUID id, AuthenticatedUser actor) {
        Maintenance maintenance = findById(id, actor);

        try {
            repo.delete(maintenance);
            repo.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new RelatedRecordsException(
                    "No se puede eliminar el mantenimiento porque tiene comentarios o evidencias asociadas.");
        }

        registerAudit(maintenance, AuditEntry.ACTION_DELETE, actor,
                "Mantenimiento eliminado (" + maintenance.getType() + ")", null);
    }

    private Client resolveClient(UUID clientId) {
        return clientRepo.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
    }

    private User resolveTechnician(UUID technicianId) {
        return userRepo.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Tecnico no encontrado"));
    }

    private void registerAudit(Maintenance maintenance, String action, AuthenticatedUser actor,
                               String summary, Object payload) {
        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(maintenance.getId().toString())
                .action(action)
                .user(actor.email())
                .summary(summary)
                .payload(payload)
                .build());
    }
}
