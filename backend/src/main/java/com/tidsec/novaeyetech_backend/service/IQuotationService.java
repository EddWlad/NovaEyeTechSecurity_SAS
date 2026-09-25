package com.tidsec.novaeyetech_backend.service;

import com.tidsec.novaeyetech_backend.dto.QuotationRequest;
import com.tidsec.novaeyetech_backend.dto.QuotationStatusRequest;
import com.tidsec.novaeyetech_backend.model.Quotation;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Motor de cotizaciones.
 *
 * <p>Todas las lecturas reciben el usuario autenticado porque el alcance depende del rol: un TECNICO
 * solo ve sus propias cotizaciones.
 */
public interface IQuotationService {

    Quotation create(QuotationRequest request, AuthenticatedUser actor);

    List<Quotation> findAll(AuthenticatedUser actor);

    Page<Quotation> findAll(AuthenticatedUser actor, Pageable pageable);

    Quotation findById(UUID id, AuthenticatedUser actor);

    /** Solo se puede editar una cotizacion en estado BORRADOR. */
    Quotation updateDraft(UUID id, QuotationRequest request, AuthenticatedUser actor);

    Quotation updateStatus(UUID id, QuotationStatusRequest request, AuthenticatedUser actor);

    byte[] buildPdf(UUID id, AuthenticatedUser actor);

    /** Paginas del mismo PDF como imagenes PNG, para la vista previa en celulares. */
    List<byte[]> buildPdfPreview(UUID id, AuthenticatedUser actor);
}
