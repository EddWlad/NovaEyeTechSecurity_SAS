import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { Maintenance, Quotation } from '../../../core/models/entities.models';

export interface DashboardSummary {
  clients: number;
  suppliers: number;
  products: number;
  services: number;
  pendingMaintenance: number;
  quotations: Quotation[];
  maintenance: Maintenance[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(private readonly api: ApiService) {}

  /** Una sola petición: el backend calcula los totales y devuelve los 5 registros recientes. */
  loadDashboard(): Observable<DashboardSummary> {
    return this.api.getOne<DashboardSummary>('dashboard/summary');
  }
}
