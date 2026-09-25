import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import {
  Attachment,
  Maintenance,
  MaintenanceComment,
  User,
} from '../../../core/models/entities.models';
import { environment } from '../../../../environments/environment';

export interface MaintenancePayload {
  clientId: string;
  type: 'PREVENTIVO' | 'CORRECTIVO';
  status: 'PENDIENTE' | 'EN_PROCESO' | 'COMPLETADO' | 'CANCELADO';
  scheduledDate: string;
  executionDate?: string;
  technicianId?: string;
  intervenedSystem: string;
  diagnosis: string;
  appliedSolution: string;
  observations?: string;
}

@Injectable({ providedIn: 'root' })
export class MaintenanceService {
  constructor(
    private readonly api: ApiService,
    private readonly http: HttpClient,
  ) {}

  list(page = 1, limit = 10) {
    return this.api.listPaginated<Maintenance>('maintenance', { page, limit });
  }

  findOne(id: string) {
    return this.api.get<Maintenance>('maintenance', id);
  }

  create(payload: MaintenancePayload) {
    return this.api.post<Maintenance>('maintenance', payload);
  }

  update(id: string, payload: Partial<MaintenancePayload>) {
    return this.api.patchById<Maintenance>('maintenance', id, payload);
  }

  delete(id: string) {
    return this.api.delete('maintenance', id);
  }

  listUsers() {
    return this.api.list<User>('users');
  }

  comments(maintenanceId: string) {
    return this.api.list<MaintenanceComment>(`maintenance-comments/maintenance/${maintenanceId}`);
  }

  addComment(maintenanceId: string, comment: string) {
    return this.api.post<MaintenanceComment>('maintenance-comments', { maintenanceId, comment });
  }

  attachments(sourceEntity: string, sourceEntityId: string) {
    return this.api.list<Attachment>(`attachments/${sourceEntity}/${sourceEntityId}`);
  }

  uploadAttachment(maintenanceId: string, file: File): Observable<Attachment> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<Attachment>(
      `${environment.apiBaseUrl}/attachments/upload/maintenance/${maintenanceId}`,
      formData,
    );
  }

  uploadAttachments(maintenanceId: string, files: File[]) {
    return forkJoin(
      files.map((file) => this.uploadAttachment(maintenanceId, file)),
    );
  }

  downloadAttachmentBlob(attachmentId: string) {
    return this.http.get(`${environment.apiBaseUrl}/attachments/${attachmentId}/download`, {
      responseType: 'blob',
    });
  }
}
