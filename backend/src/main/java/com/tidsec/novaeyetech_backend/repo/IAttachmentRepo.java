package com.tidsec.novaeyetech_backend.repo;

import com.tidsec.novaeyetech_backend.model.Attachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public interface IAttachmentRepo extends IGenericRepo<Attachment, UUID> {

    List<Attachment> findBySourceEntityAndSourceEntityId(String sourceEntity, String sourceEntityId, Sort sort);

    Page<Attachment> findBySourceEntityAndSourceEntityId(String sourceEntity, String sourceEntityId, Pageable pageable);
}
