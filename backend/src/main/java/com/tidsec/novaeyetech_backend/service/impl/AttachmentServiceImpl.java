package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.dto.common.AuditEntry;
import com.tidsec.novaeyetech_backend.exception.BusinessRuleException;
import com.tidsec.novaeyetech_backend.exception.ResourceNotFoundException;
import com.tidsec.novaeyetech_backend.model.Attachment;
import com.tidsec.novaeyetech_backend.repo.IAttachmentRepo;
import com.tidsec.novaeyetech_backend.repo.IMaintenanceRepo;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IAttachmentService;
import com.tidsec.novaeyetech_backend.service.IAuditLogService;
import com.tidsec.novaeyetech_backend.service.IStorageService;
import com.tidsec.novaeyetech_backend.util.PaginationSupport;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Adjuntos y evidencias.
 *
 * <p>Los archivos se guardan en Cloudinary a traves de {@link IStorageService}; en la base solo
 * queda el metadato y la URL. El servidor no escribe nada en disco, de modo que la aplicacion es
 * apta para un despliegue con sistema de archivos efimero.
 *
 * <p>La descarga sigue pasando por el backend en vez de redirigir al cliente: mantiene el endpoint
 * autenticado y conserva el contrato que ya consume el frontend.
 */
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements IAttachmentService {

    private static final String MODULE = "attachments";
    private static final String ENTITY = "Attachment";
    private static final String MAINTENANCE_SOURCE = "maintenance";

    private final IAttachmentRepo repo;
    private final IMaintenanceRepo maintenanceRepo;
    private final IAuditLogService auditLogService;
    private final IStorageService storageService;

    @Override
    @Transactional
    public Attachment createFromUpload(String sourceEntity, String sourceEntityId,
                                       MultipartFile file, AuthenticatedUser actor) {
        String normalizedSource = normalizeSourceEntity(sourceEntity);
        validateSourceAssociation(normalizedSource, sourceEntityId);

        IStorageService.StoredFile stored = storageService.upload(file, normalizedSource);

        Attachment attachment = Attachment.builder()
                .sourceEntity(normalizedSource)
                .sourceEntityId(sourceEntityId)
                .originalName(originalNameOf(file))
                .storedName(stored.storedName())
                .mimeType(stored.mimeType())
                .storagePath(stored.url())
                .publicId(stored.publicId())
                .resourceType(stored.resourceType())
                .size(stored.size())
                .uploadedBy(actor.email())
                .build();

        return persist(attachment, actor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attachment> findBySource(String sourceEntity, String sourceEntityId) {
        return repo.findBySourceEntityAndSourceEntityId(
                normalizeSourceEntity(sourceEntity), sourceEntityId, PaginationSupport.newestFirst());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Attachment> findBySource(String sourceEntity, String sourceEntityId, Pageable pageable) {
        return repo.findBySourceEntityAndSourceEntityId(
                normalizeSourceEntity(sourceEntity), sourceEntityId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadedAttachment download(UUID id) {
        Attachment attachment = findById(id);

        return new DownloadedAttachment(attachment, storageService.download(attachment.getStoragePath()));
    }

    /**
     * Borra el registro y, si se puede, tambien el recurso remoto.
     *
     * <p>El orden importa: primero se elimina en Cloudinary y despues en la base. Si el remoto falla
     * el registro igual se borra, porque un archivo huerfano en el proveedor es menos danino que una
     * fila que apunta a algo que el usuario cree eliminado.
     */
    @Override
    @Transactional
    public void delete(UUID id, AuthenticatedUser actor) {
        Attachment attachment = findById(id);

        storageService.delete(attachment.getPublicId(), attachment.getResourceType());
        repo.delete(attachment);

        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(id.toString())
                .action(AuditEntry.ACTION_DELETE)
                .user(actor.email())
                .summary("Adjunto eliminado: " + attachment.getOriginalName())
                .build());
    }

    private Attachment findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adjunto no encontrado"));
    }

    private Attachment persist(Attachment attachment, AuthenticatedUser actor) {
        Attachment saved = repo.save(attachment);

        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(saved.getId().toString())
                .action(AuditEntry.ACTION_CREATE)
                .user(actor.email())
                .summary("Adjunto registrado en " + saved.getSourceEntity() + " (" + saved.getSourceEntityId() + ")")
                .payload(saved.getStoragePath())
                .build());

        return saved;
    }

    private void validateSourceAssociation(String sourceEntity, String sourceEntityId) {
        if (sourceEntityId == null || sourceEntityId.isBlank()) {
            throw new BusinessRuleException("sourceEntityId es requerido");
        }

        if (MAINTENANCE_SOURCE.equals(sourceEntity) && !maintenanceRepo.existsById(parseUuid(sourceEntityId))) {
            throw new ResourceNotFoundException("Mantenimiento no encontrado para adjuntar evidencia");
        }
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("sourceEntityId no es un identificador valido");
        }
    }

    private String normalizeSourceEntity(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /** El nombre del cliente solo se conserva como metadato: nunca se usa como ruta ni como archivo. */
    private String originalNameOf(MultipartFile file) {
        String name = file.getOriginalFilename();

        if (name == null || name.isBlank()) {
            return "archivo";
        }

        String normalized = name.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1).trim();

        return normalized.isEmpty() ? "archivo" : normalized;
    }
}
