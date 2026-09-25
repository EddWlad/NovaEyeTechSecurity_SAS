import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { ApiService } from '../../../core/services/api.service';
import { Quotation, QuotationSetting } from '../../../core/models/entities.models';
import { QuotationStatus } from '../../../core/models/enums';
import { environment } from '../../../../environments/environment';

export interface QuotationItemPayload {
  itemType: 'PRODUCTO' | 'SERVICIO';
  productId?: string;
  serviceId?: string;
  description?: string;
  quantity: string;
  vatPercent?: string;
  marginPercent?: string;
}

export interface CreateQuotationPayload {
  clientId: string;
  issuedAt?: string;
  validUntil?: string;
  status?: QuotationStatus;
  observations?: string;
  discount?: string;
  currency?: string;
  items: QuotationItemPayload[];
}

@Injectable({ providedIn: 'root' })
export class QuotationsService {
  private readonly api = inject(ApiService);
  private readonly http = inject(HttpClient);

  /** Busqueda por numero o cliente y filtro de estado: los resuelve el servidor sobre todas las paginas. */
  list(page = 1, limit = 10, filters: { search?: string; status?: string } = {}) {
    return this.api.listPaginated<Quotation>('quotations', {
      page,
      limit,
      search: filters.search?.trim(),
      status: filters.status,
    });
  }

  findOne(id: string) {
    return this.api.get<Quotation>('quotations', id);
  }

  create(payload: CreateQuotationPayload) {
    return this.api.post<Quotation>('quotations', payload);
  }

  updateDraft(id: string, payload: CreateQuotationPayload) {
    return this.api.patch<Quotation>(`quotations/${id}`, payload);
  }

  updateStatus(id: string, status: QuotationStatus) {
    return this.api.patch<Quotation>(`quotations/${id}/status`, { status });
  }

  getPdfUrl(id: string): string {
    return `${environment.apiBaseUrl}/quotations/${id}/pdf`;
  }

  getPdfBlob(id: string) {
    return this.http.get(this.getPdfUrl(id), { responseType: 'blob' });
  }

  /** Paginas del PDF como imagenes (data URL PNG), para dispositivos que no muestran un PDF embebido. */
  getPdfPreview(id: string) {
    return this.api.getOne<{ pages: string[] }>(`quotations/${id}/pdf/preview`);
  }

  getSettings() {
    return this.api.getOne<QuotationSetting>('quotation-settings');
  }
}
