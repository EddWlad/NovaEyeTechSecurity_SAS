import { Injectable } from '@angular/core';

import { ApiService } from '../../../core/services/api.service';
import { QuotationSetting } from '../../../core/models/entities.models';

@Injectable({ providedIn: 'root' })
export class QuotationSettingsService {
  constructor(private readonly api: ApiService) {}

  getCurrent() {
    return this.api.getOne<QuotationSetting>('quotation-settings');
  }

  update(payload: {
    currentVat: string;
    allowedVatRates: string[];
    allowedMargins: string[];
    defaultMargin: string;
    defaultCurrency: string;
    defaultValidityDays: number;
  }) {
    return this.api.patch<QuotationSetting>('quotation-settings', payload);
  }
}
