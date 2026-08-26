package com.tidsec.novaeyetech_backend.repo;

import com.tidsec.novaeyetech_backend.model.Quotation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public interface IQuotationRepo extends IGenericRepo<Quotation, UUID> {

    List<Quotation> findByCreatedByUser_Id(UUID userId, Sort sort);

    Page<Quotation> findByCreatedByUser_Id(UUID userId, Pageable pageable);

    Optional<Quotation> findByIdAndCreatedByUser_Id(UUID id, UUID userId);
}
