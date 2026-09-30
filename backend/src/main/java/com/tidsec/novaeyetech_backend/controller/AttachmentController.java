package com.tidsec.novaeyetech_backend.controller;

import com.tidsec.novaeyetech_backend.dto.AttachmentDTO;
import com.tidsec.novaeyetech_backend.dto.common.MessageResponse;
import com.tidsec.novaeyetech_backend.dto.common.PageResponse;
import com.tidsec.novaeyetech_backend.dto.common.PaginationQuery;
import com.tidsec.novaeyetech_backend.model.Attachment;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IAttachmentService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import com.tidsec.novaeyetech_backend.util.PaginationSupport;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Adjuntos y evidencias.
 *
 * <p>El orden de las rutas importa: {@code /{id}/download} y {@code /upload} se declaran antes que
 * {@code /{sourceEntity}/{sourceEntityId}}, que de otro modo las capturaria.
 */
@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private static final String MAINTENANCE_SOURCE = "maintenance";

    private final IAttachmentService service;
    private final DtoMapper dtoMapper;

    @PostMapping("/upload/maintenance/{maintenanceId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_OPERATIVO', 'TECNICO')")
    public ResponseEntity<AttachmentDTO> uploadMaintenanceEvidence(@PathVariable UUID maintenanceId,
                                                                   @RequestParam("file") MultipartFile file,
                                                                   @AuthenticationPrincipal AuthenticatedUser actor) {
        Attachment attachment = service.createFromUpload(
                MAINTENANCE_SOURCE, maintenanceId.toString(), file, actor);

        return ResponseEntity.ok(dtoMapper.map(attachment, AttachmentDTO.class));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_OPERATIVO', 'TECNICO')")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        IAttachmentService.DownloadedAttachment downloaded = service.download(id);
        Attachment attachment = downloaded.attachment();
        String encodedName = URLEncoder.encode(attachment.getOriginalName(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.parseMediaType(attachment.getMimeType()))
                .body(downloaded.content());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_OPERATIVO', 'TECNICO')")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id,
                                                  @AuthenticationPrincipal AuthenticatedUser actor) {
        service.delete(id, actor);

        return ResponseEntity.ok(new MessageResponse("Adjunto eliminado correctamente"));
    }

    @GetMapping("/{sourceEntity}/{sourceEntityId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMIN_OPERATIVO', 'TECNICO')")
    public ResponseEntity<Object> findBySource(@PathVariable String sourceEntity,
                                               @PathVariable String sourceEntityId,
                                               @Valid PaginationQuery query) {
        if (!query.isPaginated()) {
            return ResponseEntity.ok(dtoMapper.mapList(
                    service.findBySource(sourceEntity, sourceEntityId), AttachmentDTO.class));
        }

        Page<Attachment> page = service.findBySource(sourceEntity, sourceEntityId,
                PaginationSupport.toPageable(query, PaginationSupport.DEFAULT_LIMIT));

        return ResponseEntity.ok(
                PageResponse.from(page, dtoMapper.mapList(page.getContent(), AttachmentDTO.class)));
    }
}
