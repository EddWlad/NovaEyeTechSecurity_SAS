import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/services/api.service';
import { AuditLog } from '../../../core/models/entities.models';

@Injectable({ providedIn: 'root' })
export class AuditLogsService {
  private readonly api = inject(ApiService);

  list(page = 1, limit = 20) {
    return this.api.listPaginated<AuditLog>('audit-logs', { page, limit });
  }
}
