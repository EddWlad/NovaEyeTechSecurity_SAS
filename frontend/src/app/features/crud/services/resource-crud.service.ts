import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { ResourceDefinition } from '../../../core/models/resource.models';
import { RESOURCE_DEFINITIONS } from '../../../core/config/resource-definitions';
import { PaginatedResponse } from '../../../core/models/pagination.models';

@Injectable({ providedIn: 'root' })
export class ResourceCrudService {
  private readonly api = inject(ApiService);

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
    search = '',
  ): Observable<PaginatedResponse<T>> {
    const def = this.getDefinition(resourceKey);
    return this.api.listPaginated<T>(def.endpoint, { page, limit, search: search.trim() });
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

  /** Sube la imagen del registro al almacenamiento (Cloudinary) a través del backend. */
  uploadImage<T>(resourceKey: string, id: string, file: File): Observable<T> {
    const def = this.getDefinition(resourceKey);
    const body = new FormData();
    body.append('file', file);
    return this.api.post<T>(`${def.endpoint}/${id}/image`, body);
  }

  removeImage<T>(resourceKey: string, id: string): Observable<T> {
    const def = this.getDefinition(resourceKey);
    return this.api.remove<T>(`${def.endpoint}/${id}/image`);
  }
}
