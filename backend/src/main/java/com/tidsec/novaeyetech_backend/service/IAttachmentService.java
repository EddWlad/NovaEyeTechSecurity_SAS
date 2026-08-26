package com.tidsec.novaeyetech_backend.service;

import com.tidsec.novaeyetech_backend.model.Attachment;
import com.tidsec.novaeyetech_backend.dto.AttachmentRequest;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface IAttachmentService {

    Attachment create(AttachmentRequest request, AuthenticatedUser actor);

    Attachment createFromUpload(String sourceEntity, String sourceEntityId, MultipartFile file, AuthenticatedUser actor);

    List<Attachment> findBySource(String sourceEntity, String sourceEntityId);

    Page<Attachment> findBySource(String sourceEntity, String sourceEntityId, Pageable pageable);

    /** Devuelve el adjunto junto con su contenido descargado del almacenamiento. */
    DownloadedAttachment download(UUID id);

    /** Borra el registro y el recurso remoto. */
    void delete(UUID id, AuthenticatedUser actor);

    record DownloadedAttachment(Attachment attachment, byte[] content) {
    }
}
