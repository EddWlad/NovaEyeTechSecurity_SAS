import { DatePipe, NgFor, NgIf } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Maintenance } from '../../../../core/models/entities.models';
import { MaintenanceStatus, MaintenanceType } from '../../../../core/models/enums';
import { NotificationService } from '../../../../core/services/notification.service';
import { EmptyStateComponent } from '../../../../shared/components/empty-state.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { PaginationControlsComponent } from '../../../../shared/components/pagination-controls.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge.component';
import { MaintenanceService } from '../../services/maintenance.service';
import { onSearchChange } from '../../../../core/utils/search.util';

@Component({
  selector: 'app-maintenance-list-page',
  standalone: true,
  imports: [
    NgFor,
    NgIf,
    FormsModule,
    RouterLink,
    DatePipe,
    PageHeaderComponent,
    StatusBadgeComponent,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    PaginationControlsComponent,
  ],
  templateUrl: './maintenance-list-page.component.html',
  styleUrl: './maintenance-list-page.component.scss',
})
export class MaintenanceListPageComponent {
  private readonly maintenanceService = inject(MaintenanceService);
  private readonly notifications = inject(NotificationService);

  readonly loading = signal(true);
  readonly rows = signal<Maintenance[]>([]);
  readonly query = signal('');
  readonly page = signal(1);
  readonly limit = signal(10);
  readonly total = signal(0);
  readonly totalPages = signal(1);
  readonly typeFilter = signal('');
  readonly statusFilter = signal('');
  readonly showDeleteModal = signal(false);
  readonly pendingDeleteId = signal<string | null>(null);
  readonly pendingDeleteLabel = signal('este mantenimiento');

  readonly types: MaintenanceType[] = ['PREVENTIVO', 'CORRECTIVO'];
  readonly statuses: MaintenanceStatus[] = ['PENDIENTE', 'EN_PROCESO', 'COMPLETADO', 'CANCELADO'];

  private loadSubscription?: Subscription;

  constructor() {
    // Busqueda, tipo y estado los resuelve el servidor sobre todos los registros, no solo la pagina visible.
    onSearchChange(this.query, () => this.load(1));
    this.load();
  }

  changeType(type: string): void {
    this.typeFilter.set(type);
    this.load(1);
  }

  changeStatus(status: string): void {
    this.statusFilter.set(status);
    this.load(1);
  }

  load(nextPage = this.page()): void {
    this.loading.set(true);
    this.loadSubscription?.unsubscribe();
    this.loadSubscription = this.maintenanceService
      .list(nextPage, this.limit(), { search: this.query(), type: this.typeFilter(), status: this.statusFilter() })
      .subscribe({
      next: (response) => {
        this.rows.set(response.items);
        this.page.set(response.page);
        this.limit.set(response.limit);
        this.total.set(response.total);
        this.totalPages.set(response.totalPages);
      },
      error: () => this.loading.set(false),
      complete: () => this.loading.set(false),
    });
  }

  changePage(nextPage: number): void {
    this.load(nextPage);
  }

  remove(item: Maintenance): void {
    this.pendingDeleteId.set(String(item.id));
    this.pendingDeleteLabel.set(
      item?.intervenedSystem
        ? `${item.intervenedSystem} (${item.client?.nameOrBusinessName ?? 'sin cliente'})`
        : 'este mantenimiento',
    );
    this.showDeleteModal.set(true);
  }

  closeDeleteModal(): void {
    this.showDeleteModal.set(false);
    this.pendingDeleteId.set(null);
  }

  confirmDelete(): void {
    const id = this.pendingDeleteId();
    if (!id) {
      return;
    }

    this.closeDeleteModal();
    this.maintenanceService.delete(id).subscribe({
      next: () => {
        this.notifications.success('Mantenimiento eliminado correctamente.');
        const shouldGoPrevious =
          this.rows().length === 1 && this.page() > 1;
        this.load(shouldGoPrevious ? this.page() - 1 : this.page());
      },
    });
  }

}
