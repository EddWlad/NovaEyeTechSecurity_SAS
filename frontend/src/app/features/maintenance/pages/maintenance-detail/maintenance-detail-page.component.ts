import { DatePipe, NgFor, NgIf } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Attachment, Maintenance, MaintenanceComment } from '../../../../core/models/entities.models';
import { NotificationService } from '../../../../core/services/notification.service';
import { MAX_UPLOAD_BYTES, tooLargeMessage } from '../../../../core/utils/image.util';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge.component';
import { MaintenanceService } from '../../services/maintenance.service';

@Component({
  selector: 'app-maintenance-detail-page',
  standalone: true,
  imports: [
    NgFor,
    NgIf,
    RouterLink,
    ReactiveFormsModule,
    DatePipe,
    PageHeaderComponent,
    StatusBadgeComponent,
    LoadingSpinnerComponent,
  ],
  templateUrl: './maintenance-detail-page.component.html',
  styleUrl: './maintenance-detail-page.component.scss',
})
export class MaintenanceDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly maintenanceService = inject(MaintenanceService);
  private readonly notifications = inject(NotificationService);

  readonly loading = signal(true);
  readonly uploadingAttachment = signal(false);
  readonly maintenance = signal<Maintenance | null>(null);
  readonly comments = signal<MaintenanceComment[]>([]);
  readonly attachments = signal<Attachment[]>([]);
  readonly selectedFiles = signal<File[]>([]);

  readonly commentForm = this.fb.group({
    comment: ['', [Validators.required, Validators.minLength(2)]],
  });

  readonly maintenanceId = this.route.snapshot.paramMap.get('id') ?? '';

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);

    this.maintenanceService.findOne(this.maintenanceId).subscribe({
      next: (maintenance) => this.maintenance.set(maintenance),
    });

    this.maintenanceService.comments(this.maintenanceId).subscribe({
      next: (comments) => this.comments.set(comments),
    });

    this.maintenanceService.attachments('maintenance', this.maintenanceId).subscribe({
      next: (rows) => this.attachments.set(rows),
      error: () => this.loading.set(false),
      complete: () => this.loading.set(false),
    });
  }

  addComment(): void {
    if (this.commentForm.invalid) {
      this.commentForm.markAllAsTouched();
      return;
    }

    this.maintenanceService
      .addComment(this.maintenanceId, this.commentForm.controls.comment.value ?? '')
      .subscribe({
        next: () => {
          this.commentForm.reset();
          this.notifications.success('Comentario agregado correctamente.');
          this.load();
        },
      });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    const tooLarge = files.filter((file) => file.size > MAX_UPLOAD_BYTES);

    if (tooLarge.length) {
      this.notifications.error(tooLarge.map((file) => tooLargeMessage(file.name)).join(' '));
      input.value = '';
      this.selectedFiles.set([]);
      return;
    }

    this.selectedFiles.set(files);
  }

  addAttachment(fileInput: HTMLInputElement): void {
    if (this.uploadingAttachment()) {
      return;
    }

    const files = this.selectedFiles();
    if (!files.length) {
      this.notifications.error('Selecciona un archivo para adjuntar.');
      return;
    }

    this.uploadingAttachment.set(true);
    this.maintenanceService
      .uploadAttachments(this.maintenanceId, files)
      .subscribe({
        next: () => {
          const label =
            files.length === 1
              ? 'Evidencia adjuntada correctamente.'
              : `${files.length} evidencias adjuntadas correctamente.`;

          this.notifications.success(label);
          this.selectedFiles.set([]);
          fileInput.value = '';
          this.load();
        },
        error: (error: HttpErrorResponse) => {
          this.uploadingAttachment.set(false);
          const message =
            (error.error as { message?: string | string[] })?.message ?? '';
          const readable =
            Array.isArray(message) ? message.join(' | ') : message;
          this.notifications.error(
            readable ||
              'No se pudo subir la evidencia. Verifica el archivo e inténtalo nuevamente.',
          );
        },
        complete: () => this.uploadingAttachment.set(false),
      });
  }

  selectedFilesLabel(): string {
    const files = this.selectedFiles();
    if (!files.length) {
      return 'No se ha seleccionado ningún archivo';
    }
    if (files.length === 1) {
      return files[0].name;
    }
    return `${files.length} archivos seleccionados`;
  }

  selectedFilesTotalSize(): number {
    return this.selectedFiles().reduce((sum, file) => sum + file.size, 0);
  }

  /**
   * Los adjuntos viven en Cloudinary y `storagePath` es su URL. Los registros anteriores a esa
   * migración guardan una ruta de disco que ya no existe: para esos no hay vista previa.
   */
  isRemote(storagePath: string | null | undefined): boolean {
    return !!storagePath && storagePath.startsWith('http');
  }

  isImage(mimeType: string | null | undefined): boolean {
    return !!mimeType && mimeType.startsWith('image/');
  }

  downloadAttachment(attachment: {
    id: string;
    originalName: string;
  }): void {
    this.maintenanceService.downloadAttachmentBlob(attachment.id).subscribe({
      next: (blob) => {
        const objectUrl = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = objectUrl;
        link.download = attachment.originalName || 'evidencia';
        document.body.append(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(objectUrl);
      },
    });
  }
}
