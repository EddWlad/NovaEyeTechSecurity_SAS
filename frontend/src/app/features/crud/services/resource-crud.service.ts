import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { ResourceDefinition } from '../../../core/models/resource.models';
import { RESOURCE_DEFINITIONS } from '../../../core/config/resource-definitions';
import { PaginatedResponse } from '../../../core/models/pagination.models';

@Injectable({ providedIn: 'root' })
export class ResourceCrudService {
  constructor(private readonly api: ApiService) {}

  getDefinition(key: string): ResourceDefinition {
    const definition = RESOURCE_DEFINITIONS[key];
    if (!definition) {
      throw new Error(`Recurso no soportado: ${key}`);
    }
    return definition;
  }

  list<T>(resourceKey: string): Observable<T[]> {
    const def = this.getDefinition(resourceKey);
    return this.api.list<T>(def.endpoint);
  }

  listPaginated<T>(
    resourceKey: string,
    page: number,
    limit: number,
  ): Observable<PaginatedResponse<T>> {
    const def = this.getDefinition(resourceKey);
    return this.api.listPaginated<T>(def.endpoint, { page, limit });
  }

  get<T>(resourceKey: string, id: string): Observable<T> {
    const def = this.getDefinition(resourceKey);
    return this.api.get<T>(def.endpoint, id);
  }

  create<T>(resourceKey: string, payload: Record<string, unknown>): Observable<T> {
    const def = this.getDefinition(resourceKey);
    return this.api.post<T>(def.endpoint, payload);
  }

  update<T>(resourceKey: string, id: string, payload: Record<string, unknown>): Observable<T> {
    const def = this.getDefinition(resourceKey);
    return this.api.patchById<T>(def.endpoint, id, payload);
  }

  delete(resourceKey: string, id: string) {
    const def = this.getDefinition(resourceKey);
    return this.api.delete(def.endpoint, id);
  }
}
