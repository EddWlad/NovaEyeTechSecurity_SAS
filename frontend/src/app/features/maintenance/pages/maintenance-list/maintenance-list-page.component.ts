import { DatePipe, NgFor, NgIf } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { MaintenanceStatus, MaintenanceType } from '../../../../core/models/enums';
import { NotificationService } from '../../../../core/services/notification.service';
import { toDateInputValue } from '../../../../core/utils/format.util';
import { EmptyStateComponent } from '../../../../shared/components/empty-state.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { PaginationControlsComponent } from '../../../../shared/components/pagination-controls.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge.component';
import { MaintenanceService } from '../../services/maintenance.service';

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
  readonly rows = signal<any[]>([]);
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

  readonly filtered = computed(() => {
    const query = this.query().toLowerCase();
    const type = this.typeFilter();
    const status = this.statusFilter();

    return this.rows().filter((item) => {
      const queryMatch =
        !query ||
        item.client?.nameOrBusinessName?.toLowerCase().includes(query) ||
        item.intervenedSystem?.toLowerCase().includes(query);
      const typeMatch = !type || item.type === type;
      const statusMatch = !status || item.status === status;
      return queryMatch && typeMatch && statusMatch;
    });
  });

  constructor() {
    this.load();
  }

  load(nextPage = this.page()): void {
    this.loading.set(true);
    this.maintenanceService.list(nextPage, this.limit()).subscribe({
      next: (response) => {
        this.rows.set(response.items);
        this.page.set(response.page);
        this.limit.set(response.limit);
        this.total.set(response.total);
        this.totalPages.set(response.totalPages);
      },
      complete: () => this.loading.set(false),
    });
  }

  changePage(nextPage: number): void {
    this.load(nextPage);
  }

  remove(item: any): void {
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
          this.rows().length === 1 && this.page() > 1 && this.filtered().length <= 1;
        this.load(shouldGoPrevious ? this.page() - 1 : this.page());
      },
    });
  }

  today(): string {
    return toDateInputValue(new Date());
  }
}
