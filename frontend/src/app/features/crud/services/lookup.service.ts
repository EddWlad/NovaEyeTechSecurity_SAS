import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../../core/services/api.service';

@Injectable({ providedIn: 'root' })
export class LookupService {
  private readonly api = inject(ApiService);

  list(endpoint: string) {
    return this.api.list<Record<string, unknown>>(endpoint);
  }
}
