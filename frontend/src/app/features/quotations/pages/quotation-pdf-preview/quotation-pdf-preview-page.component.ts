import { NgFor, NgIf } from '@angular/common';
import { Component, OnDestroy, SecurityContext, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Observable, map, of, tap } from 'rxjs';

import { EmptyStateComponent } from '../../../../shared/components/empty-state.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { QuotationsService } from '../../services/quotations.service';
import { Quotation } from '../../../../core/models/entities.models';
import { NotificationService } from '../../../../core/services/notification.service';
import { toMoney } from '../../../../core/utils/format.util';

/**
 * Los navegadores de celular (Chrome en Android, por ejemplo) no dibujan un PDF embebido en la pagina:
 * muestran una tarjeta generica con el nombre del blob, que es un UUID. Ahi se usa la vista previa en
 * imagenes que genera el backend. `pdfViewerEnabled` no basta: Safari en iPhone lo informa como
 * disponible pero solo muestra la primera pagina.
 */
const canEmbedPdf = (): boolean =>
  navigator.pdfViewerEnabled === true && !window.matchMedia('(max-width: 768px), (pointer: coarse)').matches;

@Component({
  selector: 'app-quotation-pdf-preview-page',
  standalone: true,
  imports: [NgIf, NgFor, RouterLink, PageHeaderComponent, LoadingSpinnerComponent, EmptyStateComponent],
  templateUrl: './quotation-pdf-preview-page.component.html',
  styleUrl: './quotation-pdf-preview-page.component.scss',
})
export class QuotationPdfPreviewPageComponent implements OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly quotationsService = inject(QuotationsService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly notifications = inject(NotificationService);

  /** true: visor PDF nativo embebido (escritorio). false: paginas como imagenes (celular). */
  readonly embedPdf = canEmbedPdf();
  /** Compartir el PDF con las apps del celular (WhatsApp, correo) cuando el navegador lo permite. */
  readonly canShareFiles =
    !this.embedPdf &&
    typeof navigator.canShare === 'function' &&
    navigator.canShare({ files: [new File([], 'cotizacion.pdf', { type: 'application/pdf' })] });

  readonly loading = signal(true);
  readonly failed = signal(false);
  readonly working = signal(false);
  readonly safePdfUrl = signal<SafeResourceUrl | null>(null);
  readonly pages = signal<string[]>([]);
  readonly fileName = signal('cotizacion.pdf');
  readonly quotation = signal<Quotation | null>(null);

  private pdfBlob: Blob | null = null;
  private objectUrl: string | null = null;

  readonly quotationId = this.route.snapshot.paramMap.get('id') ?? '';

  constructor() {
    this.quotationsService.findOne(this.quotationId).subscribe({
      next: (quotation) => {
        this.quotation.set(quotation);
        this.fileName.set(this.buildFileName(quotation));
      },
    });

    if (this.embedPdf) {
      this.loadBlob().subscribe({
        next: (url) => this.safePdfUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(url)),
        error: () => this.fail(),
        complete: () => this.loading.set(false),
      });
      return;
    }

    // En celular se piden las imagenes; el PDF se descarga recien si el usuario lo pide.
    this.quotationsService.getPdfPreview(this.quotationId).subscribe({
      next: ({ pages }) => this.pages.set(pages),
      error: () => this.fail(),
      complete: () => this.loading.set(false),
    });

    // Excepcion: si se puede compartir, el PDF se precarga. navigator.share() solo funciona dentro del
    // toque del usuario, y Safari lo rechaza si antes hubo que esperar la descarga.
    if (this.canShareFiles) {
      this.loadBlob().subscribe({ error: () => undefined });
    }
  }

  money(value: string, currency: string): string {
    return toMoney(value, currency);
  }

  download(): void {
    this.withPdf((url) => {
      const link = document.createElement('a');
      link.href = url;
      link.download = this.fileName();
      document.body.append(link);
      link.click();
      link.remove();
    });
  }

  print(): void {
    this.withPdf((url) => {
      const cleanUrl = this.sanitizer.sanitize(SecurityContext.URL, url);
      if (cleanUrl) {
        window.open(cleanUrl, '_blank');
      }
    });
  }

  share(): void {
    if (!this.pdfBlob) {
      this.notifications.info('El PDF aún se está preparando. Intenta en un momento.');
      return;
    }

    this.withPdf(() => {
      const file = new File([this.pdfBlob!], this.fileName(), { type: 'application/pdf' });
      const quotation = this.quotation();
      navigator
        .share({ files: [file], title: quotation ? `Cotización ${quotation.quotationNumber}` : 'Cotización' })
        // Cerrar el menu de compartir tambien rechaza la promesa: no es un error para el usuario.
        .catch((error: DOMException) => {
          if (error?.name !== 'AbortError') {
            this.notifications.error('No se pudo compartir el PDF. Usa "Descargar".');
          }
        });
    });
  }

  ngOnDestroy(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
    }
  }

  /** Ejecuta la accion con el PDF, descargandolo la primera vez que se necesita. */
  private withPdf(action: (url: string) => void): void {
    if (this.working()) {
      return;
    }

    this.working.set(true);
    this.loadBlob().subscribe({
      next: (url) => action(url),
      error: () => this.working.set(false),
      complete: () => this.working.set(false),
    });
  }

  private loadBlob(): Observable<string> {
    if (this.objectUrl) {
      return of(this.objectUrl);
    }

    return this.quotationsService.getPdfBlob(this.quotationId).pipe(
      tap((blob) => {
        // La precarga y un toque en Descargar pueden resolver a la vez: una sola URL, para liberarla al salir.
        if (!this.objectUrl) {
          this.pdfBlob = blob;
          this.objectUrl = URL.createObjectURL(blob);
        }
      }),
      map(() => this.objectUrl!),
    );
  }

  private fail(): void {
    this.failed.set(true);
    this.loading.set(false);
  }

  private buildFileName(quotation: Quotation): string {
    const rawDate = quotation.issuedAt || new Date().toISOString().slice(0, 10);
    const date = rawDate.replaceAll('/', '-');
    const normalizedClient = quotation.client.nameOrBusinessName
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '')
      .replace(/[^a-zA-Z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '')
      .toLowerCase();

    return `cotizacion_${date}_${normalizedClient || 'cliente'}.pdf`;
  }
}
