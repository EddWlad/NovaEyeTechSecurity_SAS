package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.dto.QuotationSettingRequest;
import com.tidsec.novaeyetech_backend.dto.common.AuditEntry;
import com.tidsec.novaeyetech_backend.model.QuotationSetting;
import com.tidsec.novaeyetech_backend.repo.IQuotationSettingRepo;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IAuditLogService;
import com.tidsec.novaeyetech_backend.service.IQuotationSettingService;
import com.tidsec.novaeyetech_backend.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parametros del motor de cotizaciones.
 *
 * <p>Es una fila unica: si la tabla esta vacia se siembra con los valores por defecto en la primera
 * lectura, de modo que el sistema nunca queda sin configuracion.
 */
@Service
@RequiredArgsConstructor
public class QuotationSettingServiceImpl implements IQuotationSettingService {

    private static final String MODULE = "quotation-settings";
    private static final String ENTITY = "QuotationSetting";

    private static final BigDecimal DEFAULT_VAT = new BigDecimal("15.00");
    private static final BigDecimal DEFAULT_MARGIN = new BigDecimal("20.00");
    private static final List<String> DEFAULT_VAT_RATES = List.of("0", "1", "12", "15");
    private static final List<String> DEFAULT_MARGINS = List.of("0", "10", "12", "20", "25", "30");
    private static final String DEFAULT_CURRENCY = "USD";
    private static final int DEFAULT_VALIDITY_DAYS = 15;

    private final IQuotationSettingRepo repo;
    private final IAuditLogService auditLogService;

    @Override
    @Transactional
    public QuotationSetting getCurrentSettings() {
        return repo.findFirstByOrderByCreatedAtAsc().orElseGet(this::createDefaults);
    }

    @Override
    @Transactional
    public QuotationSetting updateSettings(QuotationSettingRequest request, AuthenticatedUser actor) {
        QuotationSetting settings = getCurrentSettings();

        if (request.getCurrentVat() != null) {
            settings.setCurrentVat(MoneyUtils.scale(request.getCurrentVat()));
        }
        if (request.getAllowedVatRates() != null) {
            settings.setAllowedVatRates(request.getAllowedVatRates());
        }
        if (request.getAllowedMargins() != null) {
            settings.setAllowedMargins(request.getAllowedMargins());
        }
        if (request.getDefaultMargin() != null) {
            settings.setDefaultMargin(MoneyUtils.scale(request.getDefaultMargin()));
        }
        if (request.getDefaultCurrency() != null) {
            settings.setDefaultCurrency(request.getDefaultCurrency().toUpperCase());
        }
        if (request.getDefaultValidityDays() != null) {
            settings.setDefaultValidityDays(request.getDefaultValidityDays());
        }

        QuotationSetting saved = repo.save(settings);
        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(saved.getId().toString())
                .action(AuditEntry.ACTION_UPDATE)
                .user(actor.email())
                .summary("Parametros de cotizacion actualizados")
                .payload(request)
                .build());

        return saved;
    }

    private QuotationSetting createDefaults() {
        return repo.save(QuotationSetting.builder()
                .currentVat(DEFAULT_VAT)
                .allowedVatRates(DEFAULT_VAT_RATES)
                .allowedMargins(DEFAULT_MARGINS)
                .defaultMargin(DEFAULT_MARGIN)
                .defaultCurrency(DEFAULT_CURRENCY)
                .defaultValidityDays(DEFAULT_VALIDITY_DAYS)
                .build());
    }
}
