package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.dto.ServiceRequest;
import com.tidsec.novaeyetech_backend.dto.common.AuditEntry;
import com.tidsec.novaeyetech_backend.exception.ResourceNotFoundException;
import com.tidsec.novaeyetech_backend.model.ServiceCategory;
import com.tidsec.novaeyetech_backend.model.ServiceItem;
import com.tidsec.novaeyetech_backend.repo.IGenericRepo;
import com.tidsec.novaeyetech_backend.repo.IServiceCategoryRepo;
import com.tidsec.novaeyetech_backend.repo.IServiceItemRepo;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IAuditLogService;
import com.tidsec.novaeyetech_backend.service.IServiceItemService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import com.tidsec.novaeyetech_backend.util.MoneyUtils;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceItemServiceImpl extends CRUDImpl<ServiceItem, UUID> implements IServiceItemService {

    private static final String MODULE = "services";
    private static final String ENTITY = "Service";

    private final IServiceItemRepo repo;
    private final IServiceCategoryRepo categoryRepo;
    private final IAuditLogService auditLogService;
    private final DtoMapper dtoMapper;

    @Override
    protected IGenericRepo<ServiceItem, UUID> getRepo() {
        return repo;
    }

    @Override
    protected String notFoundMessage() {
        return "Servicio no encontrado";
    }

    @Override
    protected String relatedRecordsMessage() {
        return "No se puede eliminar el servicio porque esta relacionado con otros registros.";
    }

    @Override
    @Transactional
    public ServiceItem create(ServiceRequest request, AuthenticatedUser actor) {
        ServiceItem service = dtoMapper.map(request, ServiceItem.class);
        service.setCategory(resolveCategory(request.getCategoryId()));
        service.setBaseCost(MoneyUtils.scale(request.getBaseCost()));
        service.setActive(request.getActive() == null || request.getActive());

        ServiceItem saved = repo.save(service);
        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(saved.getId().toString())
                .action(AuditEntry.ACTION_CREATE)
                .user(actor.email())
                .summary("Servicio creado: " + saved.getName())
                .payload(request)
                .build());

        return saved;
    }

    @Override
    @Transactional
    public ServiceItem update(UUID id, ServiceRequest request, AuthenticatedUser actor) {
        ServiceItem service = findById(id);

        dtoMapper.patch(request, service);

        if (request.getCategoryId() != null) {
            service.setCategory(resolveCategory(request.getCategoryId()));
        }
        if (request.getBaseCost() != null) {
            service.setBaseCost(MoneyUtils.scale(request.getBaseCost()));
        }
        if (request.getActive() != null) {
            service.setActive(request.getActive());
        }

        ServiceItem saved = repo.save(service);
        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(saved.getId().toString())
                .action(AuditEntry.ACTION_UPDATE)
                .user(actor.email())
                .summary("Servicio actualizado: " + saved.getName())
                .payload(request)
                .build());

        return saved;
    }

    @Override
    @Transactional
    public void delete(UUID id, AuthenticatedUser actor) {
        ServiceItem service = findById(id);
        String name = service.getName();

        delete(id);

        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(id.toString())
                .action(AuditEntry.ACTION_DELETE)
                .user(actor.email())
                .summary("Servicio eliminado: " + name)
                .build());
    }

    private ServiceCategory resolveCategory(UUID categoryId) {
        return categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria de servicio no encontrada"));
    }
}
