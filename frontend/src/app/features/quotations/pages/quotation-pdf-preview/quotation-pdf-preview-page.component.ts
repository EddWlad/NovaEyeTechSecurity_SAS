import { NgIf } from '@angular/common';
import { Component, OnDestroy, SecurityContext, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { QuotationsService } from '../../services/quotations.service';
import { Quotation } from '../../../../core/models/entities.models';
import { toMoney } from '../../../../core/utils/format.util';

@Component({
  selector: 'app-quotation-pdf-preview-page',
  standalone: true,
  imports: [NgIf, RouterLink, PageHeaderComponent, LoadingSpinnerComponent],
  templateUrl: './quotation-pdf-preview-page.component.html',
  styleUrl: './quotation-pdf-preview-page.component.scss',
})
export class QuotationPdfPreviewPageComponent implements OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly quotationsService = inject(QuotationsService);
  private readonly sanitizer = inject(DomSanitizer);

  readonly loading = signal(true);
  readonly safePdfUrl = signal<SafeResourceUrl | null>(null);
  readonly fileName = signal('cotizacion.pdf');
  readonly quotation = signal<Quotation | null>(null);
  private objectUrl: string | null = null;

  readonly quotationId = this.route.snapshot.paramMap.get('id') ?? '';

  constructor() {
    this.quotationsService.findOne(this.quotationId).subscribe({
      next: (quotation) => {
        this.quotation.set(quotation);
        this.fileName.set(this.buildFileName(quotation));
      },
    });

    this.quotationsService.getPdfBlob(this.quotationId).subscribe({
      next: (blob) => {
        this.objectUrl = URL.createObjectURL(blob);
        this.safePdfUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrl));
      },
      complete: () => this.loading.set(false),
    });
  }

  money(value: string, currency: string): string {
    return toMoney(value, currency);
  }

  download(): void {
    if (!this.objectUrl) {
      return;
    }

    const link = document.createElement('a');
    link.href = this.objectUrl;
    link.download = this.fileName();
    document.body.append(link);
    link.click();
    link.remove();
  }

  print(): void {
    const cleanUrl = this.sanitizer.sanitize(SecurityContext.URL, this.objectUrl);
    if (cleanUrl) {
      window.open(cleanUrl, '_blank');
    }
  }

  ngOnDestroy(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
    }
  }

  private buildFileName(quotation: Quotation): string {
    const rawDate = quotation.issuedAt || new Date().toISOString().slice(0, 10);
    const date = rawDate.replaceAll('/', '-');
    const normalizedClient = quotation.client.nameOrBusinessName
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-zA-Z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '')
      .toLowerCase();

    return `cotizacion_${date}_${normalizedClient || 'cliente'}.pdf`;
  }
}
