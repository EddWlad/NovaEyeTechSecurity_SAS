import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PaginatedResponse } from '../models/pagination.models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private readonly http: HttpClient) {}

  private buildHttpParams(
    params?: Record<string, string | number | boolean | undefined>,
  ): HttpParams {
    let httpParams = new HttpParams();
    Object.entries(params ?? {}).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    });
    return httpParams;
  }

  list<T>(endpoint: string, params?: Record<string, string | number | boolean | undefined>): Observable<T[]> {
    return this.http.get<T[]>(`${environment.apiBaseUrl}/${endpoint}`, {
      params: this.buildHttpParams(params),
    });
  }

  listPaginated<T>(
    endpoint: string,
    params?: Record<string, string | number | boolean | undefined>,
  ): Observable<PaginatedResponse<T>> {
    return this.http.get<PaginatedResponse<T>>(
      `${environment.apiBaseUrl}/${endpoint}`,
      {
        params: this.buildHttpParams(params),
      },
    );
  }

  get<T>(endpoint: string, id: string): Observable<T> {
    return this.http.get<T>(`${environment.apiBaseUrl}/${endpoint}/${id}`);
  }

  getOne<T>(endpoint: string): Observable<T> {
    return this.http.get<T>(`${environment.apiBaseUrl}/${endpoint}`);
  }

  post<T>(endpoint: string, payload: unknown): Observable<T> {
    return this.http.post<T>(`${environment.apiBaseUrl}/${endpoint}`, payload);
  }

  patch<T>(endpoint: string, payload: unknown): Observable<T> {
    return this.http.patch<T>(`${environment.apiBaseUrl}/${endpoint}`, payload);
  }

  patchById<T>(endpoint: string, id: string, payload: unknown): Observable<T> {
    return this.http.patch<T>(`${environment.apiBaseUrl}/${endpoint}/${id}`, payload);
  }

  delete(endpoint: string, id: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${environment.apiBaseUrl}/${endpoint}/${id}`);
  }

  absoluteGet<T>(url: string): Observable<T> {
    return this.http.get<T>(url);
  }
}
