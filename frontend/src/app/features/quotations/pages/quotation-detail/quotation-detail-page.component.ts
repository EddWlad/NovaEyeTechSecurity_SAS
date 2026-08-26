import { DatePipe, NgFor, NgIf } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Quotation } from '../../../../core/models/entities.models';
import { QuotationStatus } from '../../../../core/models/enums';
import { NotificationService } from '../../../../core/services/notification.service';
import { toMoney } from '../../../../core/utils/format.util';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge.component';
import { QuotationsService } from '../../services/quotations.service';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-quotation-detail-page',
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
  ],
  templateUrl: './quotation-detail-page.component.html',
  styleUrl: './quotation-detail-page.component.scss',
})
export class QuotationDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly quotationsService = inject(QuotationsService);
  private readonly notifications = inject(NotificationService);

  readonly loading = signal(true);
  readonly quotation = signal<Quotation | null>(null);

  readonly statuses: QuotationStatus[] = ['BORRADOR', 'ENVIADA', 'APROBADA', 'RECHAZADA'];

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.loading.set(false);
      return;
    }

    this.quotationsService.findOne(id).subscribe({
      next: (quotation) => this.quotation.set(quotation),
      complete: () => this.loading.set(false),
    });
  }

  updateStatus(status: QuotationStatus): void {
    const current = this.quotation();
    if (!current) {
      return;
    }

    this.quotationsService.updateStatus(current.id, status).subscribe({
      next: (quotation) => {
        this.quotation.set(quotation);
        this.notifications.success('Estado actualizado.');
      },
    });
  }

  money(value: string, currency: string): string {
    return toMoney(value, currency);
  }
}
