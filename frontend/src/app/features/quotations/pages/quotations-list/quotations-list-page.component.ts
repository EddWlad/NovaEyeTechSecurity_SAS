import { DatePipe, NgFor, NgIf } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Quotation } from '../../../../core/models/entities.models';
import { QuotationStatus } from '../../../../core/models/enums';
import { toMoney } from '../../../../core/utils/format.util';
import { onSearchChange } from '../../../../core/utils/search.util';
import { EmptyStateComponent } from '../../../../shared/components/empty-state.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge.component';
import { PaginationControlsComponent } from '../../../../shared/components/pagination-controls.component';
import { QuotationsService } from '../../services/quotations.service';

@Component({
  selector: 'app-quotations-list-page',
  standalone: true,
  imports: [
    NgFor,
    NgIf,
    FormsModule,
    RouterLink,
    DatePipe,
    PageHeaderComponent,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    StatusBadgeComponent,
    PaginationControlsComponent,
  ],
  templateUrl: './quotations-list-page.component.html',
  styleUrl: './quotations-list-page.component.scss',
})
export class QuotationsListPageComponent {
  private readonly quotationsService = inject(QuotationsService);

  readonly loading = signal(true);
  readonly rows = signal<Quotation[]>([]);
  readonly query = signal('');
  readonly status = signal('');
  readonly page = signal(1);
  readonly limit = signal(10);
  readonly total = signal(0);
  readonly totalPages = signal(1);

  readonly statuses: QuotationStatus[] = ['BORRADOR', 'ENVIADA', 'APROBADA', 'RECHAZADA'];

  private loadSubscription?: Subscription;

  constructor() {
    // Busqueda y estado los resuelve el servidor sobre todas las cotizaciones, no solo la pagina visible.
    onSearchChange(this.query, () => this.load(1));
    this.load();
  }

  changeStatus(status: string): void {
    this.status.set(status);
    this.load(1);
  }

  load(nextPage = this.page()): void {
    this.loading.set(true);
    this.loadSubscription?.unsubscribe();
    this.loadSubscription = this.quotationsService
      .list(nextPage, this.limit(), { search: this.query(), status: this.status() })
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

  money(value: string, currency: string): string {
    return toMoney(value, currency);
  }
}
