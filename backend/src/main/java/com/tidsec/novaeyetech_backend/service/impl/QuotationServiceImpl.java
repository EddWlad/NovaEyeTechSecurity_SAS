package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.dto.QuotationRequest;
import com.tidsec.novaeyetech_backend.dto.QuotationStatusRequest;
import com.tidsec.novaeyetech_backend.dto.common.AuditEntry;
import com.tidsec.novaeyetech_backend.exception.BusinessRuleException;
import com.tidsec.novaeyetech_backend.exception.ResourceNotFoundException;
import com.tidsec.novaeyetech_backend.model.Client;
import com.tidsec.novaeyetech_backend.model.Quotation;
import com.tidsec.novaeyetech_backend.model.QuotationSetting;
import com.tidsec.novaeyetech_backend.model.User;
import com.tidsec.novaeyetech_backend.model.enums.QuotationStatus;
import com.tidsec.novaeyetech_backend.repo.IClientRepo;
import com.tidsec.novaeyetech_backend.repo.IQuotationRepo;
import com.tidsec.novaeyetech_backend.repo.IUserRepo;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IAuditLogService;
import com.tidsec.novaeyetech_backend.service.IQuotationService;
import com.tidsec.novaeyetech_backend.service.IQuotationSettingService;
import com.tidsec.novaeyetech_backend.util.MoneyUtils;
import com.tidsec.novaeyetech_backend.util.PaginationSupport;
import com.tidsec.novaeyetech_backend.util.PdfPageRenderer;
import com.tidsec.novaeyetech_backend.util.SearchSpecification;
import com.tidsec.novaeyetech_backend.util.QuotationPdfGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motor de cotizaciones.
 *
 * <p>Reglas que no se deben alterar sin pedido explicito:
 * <ul>
 *   <li>El calculo (IVA antes que ganancia) vive en {@link QuotationCalculator}.</li>
 *   <li>Una cotizacion guardada nunca se recalcula: sus importes son historicos.</li>
 *   <li>Solo se edita una cotizacion en estado BORRADOR.</li>
 *   <li>Un TECNICO solo alcanza sus propias cotizaciones; si pide otra recibe 404, no 403, para no
 *       revelar que el recurso existe.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class QuotationServiceImpl implements IQuotationService {

    private static final String MODULE = "quotations";
    private static final String ENTITY = "Quotation";
    private static final String NOT_FOUND = "Cotizacion no encontrada";
    private static final int SEQUENCE_LENGTH = 6;

    private final IQuotationRepo repo;
    private final IClientRepo clientRepo;
    private final IUserRepo userRepo;
    private final IQuotationSettingService settingService;
    private final QuotationCalculator calculator;
    private final QuotationPdfGenerator pdfGenerator;
    private final PdfPageRenderer pdfPageRenderer;
    private final IAuditLogService auditLogService;

    @Override
    @Transactional
    public Quotation create(QuotationRequest request, AuthenticatedUser actor) {
        Client client = resolveClient(request.getClientId());
        User author = userRepo.findById(actor.id())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario creador no encontrado"));

        QuotationSetting settings = settingService.getCurrentSettings();
        QuotationCalculator.Calculation calculation = calculator.calculate(request.getItems(), settings);
        BigDecimal discount = resolveDiscount(request.getDiscount(), calculation.grossTotal());
        LocalDate issuedAt = request.getIssuedAt() != null ? request.getIssuedAt() : LocalDate.now();

        Quotation quotation = Quotation.builder()
                .quotationNumber(buildQuotationNumber())
                .client(client)
                .createdByUser(author)
                .issuedAt(issuedAt)
                .validUntil(resolveValidUntil(request.getValidUntil(), issuedAt, settings))
                .status(request.getStatus() != null ? request.getStatus() : QuotationStatus.BORRADOR)
                .observations(request.getObservations())
                .subtotal(calculation.subtotalBase())
                .discount(discount)
                .vatPercentHistorical(MoneyUtils.scale(settings.getCurrentVat()))
                .vatValueHistorical(calculation.vatValueTotal())
                .total(MoneyUtils.scale(calculation.grossTotal().subtract(discount)))
                .currency(resolveCurrency(request.getCurrency(), settings))
                .build();
        quotation.replaceDetails(calculation.details());

        Quotation saved = repo.save(quotation);
        registerAudit(saved, AuditEntry.ACTION_CREATE, actor,
                "Cotizacion creada: " + saved.getQuotationNumber());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Quotation> findAll(AuthenticatedUser actor) {
        return actor.isTechnician()
                ? repo.findByCreatedByUser_Id(actor.id(), PaginationSupport.newestFirst())
                : repo.findAll(PaginationSupport.newestFirst());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Quotation> findAll(AuthenticatedUser actor, Pageable pageable) {
        return actor.isTechnician()
                ? repo.findByCreatedByUser_Id(actor.id(), pageable)
                : repo.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Quotation> findAll(AuthenticatedUser actor, String search, QuotationStatus status, Pageable pageable) {
        String term = SearchSpecification.normalize(search);
        if (term == null && status == null) {
            return findAll(actor, pageable);
        }

        // El alcance por rol va siempre: un filtro nunca amplia lo que un tecnico puede ver.
        Specification<Quotation> spec = actor.isTechnician()
                ? (root, query, cb) -> cb.equal(root.get("createdByUser").get("id"), actor.id())
                : Specification.unrestricted();
        if (term != null) {
            spec = spec.and(SearchSpecification.containsAny(term, List.of("quotationNumber", "client.nameOrBusinessName")));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return repo.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Quotation findById(UUID id, AuthenticatedUser actor) {
        return actor.isTechnician()
                ? repo.findByIdAndCreatedByUser_Id(id, actor.id())
                        .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND))
                : repo.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    @Override
    @Transactional
    public Quotation updateDraft(UUID id, QuotationRequest request, AuthenticatedUser actor) {
        Quotation quotation = findById(id, actor);

        if (quotation.getStatus() != QuotationStatus.BORRADOR) {
            throw new BusinessRuleException("Solo se puede editar una cotizacion en estado BORRADOR");
        }

        QuotationSetting settings = settingService.getCurrentSettings();
        QuotationCalculator.Calculation calculation = calculator.calculate(request.getItems(), settings);
        BigDecimal discount = resolveDiscount(request.getDiscount(), calculation.grossTotal());
        LocalDate issuedAt = request.getIssuedAt() != null ? request.getIssuedAt() : quotation.getIssuedAt();

        quotation.setClient(resolveClient(request.getClientId()));
        quotation.setIssuedAt(issuedAt);
        quotation.setValidUntil(resolveValidUntil(request.getValidUntil(), issuedAt, settings));
        quotation.setStatus(request.getStatus() != null ? request.getStatus() : quotation.getStatus());
        quotation.setObservations(request.getObservations());
        quotation.setSubtotal(calculation.subtotalBase());
        quotation.setDiscount(discount);
        quotation.setVatPercentHistorical(MoneyUtils.scale(settings.getCurrentVat()));
        quotation.setVatValueHistorical(calculation.vatValueTotal());
        quotation.setTotal(MoneyUtils.scale(calculation.grossTotal().subtract(discount)));
        quotation.setCurrency(resolveCurrency(request.getCurrency(), settings));
        quotation.replaceDetails(calculation.details());

        Quotation saved = repo.save(quotation);
        registerAudit(saved, AuditEntry.ACTION_UPDATE, actor,
                "Cotizacion actualizada: " + saved.getQuotationNumber());

        return saved;
    }

    @Override
    @Transactional
    public Quotation updateStatus(UUID id, QuotationStatusRequest request, AuthenticatedUser actor) {
        Quotation quotation = findById(id, actor);
        quotation.setStatus(request.status());

        Quotation saved = repo.save(quotation);
        registerAudit(saved, AuditEntry.ACTION_UPDATE_STATUS, actor,
                "Estado actualizado a " + request.status() + " en " + saved.getQuotationNumber());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] buildPdf(UUID id, AuthenticatedUser actor) {
        return pdfGenerator.generate(findById(id, actor));
    }

    @Override
    @Transactional(readOnly = true)
    public List<byte[]> buildPdfPreview(UUID id, AuthenticatedUser actor) {
        // Se renderiza el mismo PDF que se descarga, no una version aparte: lo que se ve es lo que se entrega.
        return pdfPageRenderer.renderPages(buildPdf(id, actor));
    }

    /**
     * Numeracion COT-anio-secuencia de 6 digitos, derivada del total de filas.
     * Al depender de {@code count()} no es segura ante creaciones concurrentes; se conserva el
     * comportamiento original y la unicidad la garantiza el indice de la columna.
     */
    private String buildQuotationNumber() {
        long next = repo.count() + 1;
        String pattern = "COT-%d-%0" + SEQUENCE_LENGTH + "d";

        return pattern.formatted(Year.now().getValue(), next);
    }

    private Client resolveClient(UUID clientId) {
        return clientRepo.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
    }

    private BigDecimal resolveDiscount(BigDecimal requested, BigDecimal grossTotal) {
        BigDecimal discount = MoneyUtils.orZero(requested);

        if (MoneyUtils.isNegative(discount)) {
            throw new BusinessRuleException("Descuento invalido");
        }
        if (discount.compareTo(grossTotal) > 0) {
            throw new BusinessRuleException("El descuento no puede ser mayor al total");
        }

        return discount;
    }

    private LocalDate resolveValidUntil(LocalDate requested, LocalDate issuedAt, QuotationSetting settings) {
        return requested != null ? requested : issuedAt.plusDays(settings.getDefaultValidityDays());
    }

    private String resolveCurrency(String requested, QuotationSetting settings) {
        return requested != null && !requested.isBlank()
                ? requested.toUpperCase()
                : settings.getDefaultCurrency();
    }

    private void registerAudit(Quotation quotation, String action, AuthenticatedUser actor, String summary) {
        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(quotation.getId().toString())
                .action(action)
                .user(actor.email())
                .summary(summary)
                .payload(Map.of(
                        "clientId", quotation.getClient().getId().toString(),
                        "items", quotation.getDetails().size(),
                        "total", quotation.getTotal().toPlainString()))
                .build());
    }
}
