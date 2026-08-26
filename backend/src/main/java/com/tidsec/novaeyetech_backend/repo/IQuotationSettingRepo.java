package com.tidsec.novaeyetech_backend.repo;

import com.tidsec.novaeyetech_backend.model.QuotationSetting;
import java.util.Optional;
import java.util.UUID;

public interface IQuotationSettingRepo extends IGenericRepo<QuotationSetting, UUID> {

    /** La configuracion es una fila unica: siempre se toma la mas antigua. */
    Optional<QuotationSetting> findFirstByOrderByCreatedAtAsc();
}
