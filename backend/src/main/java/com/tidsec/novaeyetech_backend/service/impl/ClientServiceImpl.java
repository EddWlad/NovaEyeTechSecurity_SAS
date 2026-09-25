package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.dto.ClientRequest;
import com.tidsec.novaeyetech_backend.dto.common.AuditEntry;
import com.tidsec.novaeyetech_backend.exception.DuplicateResourceException;
import com.tidsec.novaeyetech_backend.model.Client;
import com.tidsec.novaeyetech_backend.repo.IClientRepo;
import com.tidsec.novaeyetech_backend.repo.IGenericRepo;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IAuditLogService;
import com.tidsec.novaeyetech_backend.service.IClientService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl extends CRUDImpl<Client, UUID> implements IClientService {

    private static final String MODULE = "clients";
    private static final String ENTITY = "Client";

    private final IClientRepo repo;
    private final IAuditLogService auditLogService;
    private final DtoMapper dtoMapper;

    @Override
    protected IGenericRepo<Client, UUID> getRepo() {
        return repo;
    }

    @Override
    protected List<String> searchFields() {
        return List.of("nameOrBusinessName", "documentNumber", "email", "phone", "city");
    }

    @Override
    protected String notFoundMessage() {
        return "Cliente no encontrado";
    }

    @Override
    protected String relatedRecordsMessage() {
        return "No se puede eliminar el cliente porque tiene registros relacionados.";
    }

    @Override
    @Transactional
    public Client create(ClientRequest request, AuthenticatedUser actor) {
        if (repo.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new DuplicateResourceException("El documento ya esta registrado");
        }

        Client client = dtoMapper.map(request, Client.class);
        client.setEmail(normalizeEmail(request.getEmail()));
        client.setActive(request.getActive() == null || request.getActive());

        Client saved = repo.save(client);
        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(saved.getId().toString())
                .action(AuditEntry.ACTION_CREATE)
                .user(actor.email())
                .summary("Cliente creado: " + saved.getNameOrBusinessName())
                .payload(request)
                .build());

        return saved;
    }

    @Override
    @Transactional
    public Client update(UUID id, ClientRequest request, AuthenticatedUser actor) {
        Client client = findById(id);

        if (request.getDocumentNumber() != null
                && !request.getDocumentNumber().equals(client.getDocumentNumber())
                && repo.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new DuplicateResourceException("El documento ya esta registrado");
        }

        dtoMapper.patch(request, client);
        if (request.getEmail() != null) {
            client.setEmail(normalizeEmail(request.getEmail()));
        }
        if (request.getActive() != null) {
            client.setActive(request.getActive());
        }

        Client saved = repo.save(client);
        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(saved.getId().toString())
                .action(AuditEntry.ACTION_UPDATE)
                .user(actor.email())
                .summary("Cliente actualizado: " + saved.getNameOrBusinessName())
                .payload(request)
                .build());

        return saved;
    }

    @Override
    @Transactional
    public void delete(UUID id, AuthenticatedUser actor) {
        Client client = findById(id);
        String name = client.getNameOrBusinessName();

        delete(id);

        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(id.toString())
                .action(AuditEntry.ACTION_DELETE)
                .user(actor.email())
                .summary("Cliente eliminado: " + name)
                .build());
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase();
    }
}
