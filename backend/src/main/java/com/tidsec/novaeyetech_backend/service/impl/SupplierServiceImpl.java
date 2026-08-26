package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.dto.SupplierRequest;
import com.tidsec.novaeyetech_backend.dto.common.AuditEntry;
import com.tidsec.novaeyetech_backend.exception.DuplicateResourceException;
import com.tidsec.novaeyetech_backend.model.Supplier;
import com.tidsec.novaeyetech_backend.repo.IGenericRepo;
import com.tidsec.novaeyetech_backend.repo.ISupplierRepo;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IAuditLogService;
import com.tidsec.novaeyetech_backend.service.ISupplierService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl extends CRUDImpl<Supplier, UUID> implements ISupplierService {

    private static final String MODULE = "suppliers";
    private static final String ENTITY = "Supplier";

    private final ISupplierRepo repo;
    private final IAuditLogService auditLogService;
    private final DtoMapper dtoMapper;

    @Override
    protected IGenericRepo<Supplier, UUID> getRepo() {
        return repo;
    }

    @Override
    protected String notFoundMessage() {
        return "Proveedor no encontrado";
    }

    @Override
    protected String relatedRecordsMessage() {
        return "No se puede eliminar el proveedor porque esta relacionado con productos.";
    }

    @Override
    @Transactional
    public Supplier create(SupplierRequest request, AuthenticatedUser actor) {
        if (repo.existsByRuc(request.getRuc())) {
            throw new DuplicateResourceException("El RUC ya esta registrado");
        }

        Supplier supplier = dtoMapper.map(request, Supplier.class);
        supplier.setEmail(normalizeEmail(request.getEmail()));
        supplier.setActive(request.getActive() == null || request.getActive());

        Supplier saved = repo.save(supplier);
        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(saved.getId().toString())
                .action(AuditEntry.ACTION_CREATE)
                .user(actor.email())
                .summary("Proveedor creado: " + saved.getBusinessName())
                .payload(request)
                .build());

        return saved;
    }

    @Override
    @Transactional
    public Supplier update(UUID id, SupplierRequest request, AuthenticatedUser actor) {
        Supplier supplier = findById(id);

        if (request.getRuc() != null
                && !request.getRuc().equals(supplier.getRuc())
                && repo.existsByRuc(request.getRuc())) {
            throw new DuplicateResourceException("El RUC ya esta registrado");
        }

        dtoMapper.patch(request, supplier);
        if (request.getEmail() != null) {
            supplier.setEmail(normalizeEmail(request.getEmail()));
        }
        if (request.getActive() != null) {
            supplier.setActive(request.getActive());
        }

        Supplier saved = repo.save(supplier);
        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(saved.getId().toString())
                .action(AuditEntry.ACTION_UPDATE)
                .user(actor.email())
                .summary("Proveedor actualizado: " + saved.getBusinessName())
                .payload(request)
                .build());

        return saved;
    }

    @Override
    @Transactional
    public void delete(UUID id, AuthenticatedUser actor) {
        Supplier supplier = findById(id);
        String name = supplier.getBusinessName();

        delete(id);

        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(id.toString())
                .action(AuditEntry.ACTION_DELETE)
                .user(actor.email())
                .summary("Proveedor eliminado: " + name)
                .build());
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase();
    }
}
