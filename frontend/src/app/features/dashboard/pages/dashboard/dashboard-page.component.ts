import { DatePipe, NgFor, NgIf } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state.component';
import { DashboardService } from '../../services/dashboard.service';
import { Quotation, Maintenance } from '../../../../core/models/entities.models';
import { toMoney } from '../../../../core/utils/format.util';

interface DashboardState {
  clients: number;
  suppliers: number;
  products: number;
  services: number;
  pendingMaintenance: number;
  quotations: Quotation[];
  maintenance: Maintenance[];
}

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    NgFor,
    NgIf,
    RouterLink,
    DatePipe,
    PageHeaderComponent,
    StatusBadgeComponent,
    LoadingSpinnerComponent,
    EmptyStateComponent,
  ],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
})
export class DashboardPageComponent {
  private readonly dashboardService = inject(DashboardService);

  readonly loading = signal(true);
  readonly state = signal<DashboardState>({
    clients: 0,
    suppliers: 0,
    products: 0,
    services: 0,
    pendingMaintenance: 0,
    quotations: [],
    maintenance: [],
  });

  readonly kpis = [
    { key: 'clients', label: 'Clientes', icon: 'domain' },
    { key: 'suppliers', label: 'Proveedores', icon: 'local_shipping' },
    { key: 'products', label: 'Productos', icon: 'inventory_2' },
    { key: 'services', label: 'Servicios', icon: 'engineering' },
    { key: 'pendingMaintenance', label: 'Mantenimientos Pendientes', icon: 'build' },
  ] as const;

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);

    this.dashboardService.loadDashboard().subscribe({
      next: (state) => {
        this.state.set(state as DashboardState);
      },
      complete: () => {
        this.loading.set(false);
      },
    });
  }

  money(value: string): string {
    return toMoney(value, 'USD');
  }
}
