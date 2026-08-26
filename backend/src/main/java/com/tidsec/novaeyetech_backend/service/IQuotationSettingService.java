package com.tidsec.novaeyetech_backend.service;

import com.tidsec.novaeyetech_backend.dto.QuotationSettingRequest;
import com.tidsec.novaeyetech_backend.model.QuotationSetting;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;

public interface IQuotationSettingService {

    /** Devuelve la fila unica de configuracion, creandola con valores por defecto si no existe. */
    QuotationSetting getCurrentSettings();

    QuotationSetting updateSettings(QuotationSettingRequest request, AuthenticatedUser actor);
}
