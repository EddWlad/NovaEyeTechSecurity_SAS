import { Injectable } from '@angular/core';
import { forkJoin, map, of } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { Maintenance, Quotation } from '../../../core/models/entities.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(private readonly api: ApiService) {}

  loadDashboard() {
    return forkJoin({
      clients: this.api.list('clients').pipe(map((items) => items.length)),
      suppliers: this.api.list('suppliers').pipe(map((items) => items.length)),
      products: this.api.list('products').pipe(map((items) => items.length)),
      services: this.api.list('services').pipe(map((items) => items.length)),
      quotations: this.api.list<Quotation>('quotations').pipe(map((items) => items.slice(0, 5))),
      maintenance: this.api
        .list<Maintenance>('maintenance')
        .pipe(map((items) => items.filter((item) => item.status !== 'COMPLETADO').slice(0, 5))),
    }).pipe(
      map((data) => ({
        ...data,
        pendingMaintenance: data.maintenance.length,
      })),
    );
  }
}
