import { DatePipe, NgFor, NgIf } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuditLog } from '../../../../core/models/entities.models';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { PaginationControlsComponent } from '../../../../shared/components/pagination-controls.component';
import { AuditLogsService } from '../../services/audit-logs.service';

@Component({
  selector: 'app-audit-log-list-page',
  standalone: true,
  imports: [NgFor, NgIf, FormsModule, DatePipe, LoadingSpinnerComponent, EmptyStateComponent, PageHeaderComponent, PaginationControlsComponent],
  templateUrl: './audit-log-list-page.component.html',
  styleUrl: './audit-log-list-page.component.scss',
})
export class AuditLogListPageComponent {
  private readonly service = inject(AuditLogsService);

  readonly loading = signal(true);
  readonly rows = signal<AuditLog[]>([]);
  readonly page = signal(1);
  readonly limit = signal(20);
  readonly total = signal(0);
  readonly totalPages = signal(1);
  readonly moduleFilter = signal('');
  readonly userFilter = signal('');
  readonly actionFilter = signal('');

  readonly filtered = computed(() => {
    const module = this.moduleFilter().toLowerCase();
    const user = this.userFilter().toLowerCase();
    const action = this.actionFilter().toLowerCase();

    return this.rows().filter((row) => {
      const moduleOk = !module || row.module.toLowerCase().includes(module);
      const userOk = !user || row.user.toLowerCase().includes(user);
      const actionOk = !action || row.action.toLowerCase().includes(action);
      return moduleOk && userOk && actionOk;
    });
  });

  constructor() {
    this.load();
  }

  load(nextPage = this.page()): void {
    this.loading.set(true);
    this.service.list(nextPage, this.limit()).subscribe({
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
}
