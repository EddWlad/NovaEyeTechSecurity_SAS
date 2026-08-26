import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, map, of, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, SessionUser } from '../models/auth.models';
import { Role } from '../models/enums';

const TOKEN_KEY = 'nexteye_token';
const USER_KEY = 'nexteye_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly userSignal = signal<SessionUser | null>(null);
  private readonly tokenSignal = signal<string | null>(null);

  readonly user = computed(() => this.userSignal());
  readonly token = computed(() => this.tokenSignal());
  readonly isAuthenticated = computed(() => !!this.tokenSignal());

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
  ) {
    this.hydrateSession();
  }

  login(payload: LoginRequest): Observable<SessionUser> {
    return this.http
      .post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, payload)
      .pipe(
        tap((response) => this.setSession(response.access_token, response.user)),
        map((response) => response.user),
      );
  }

  refreshProfile(): Observable<SessionUser | null> {
    if (!this.tokenSignal()) {
      return of(null);
    }

    return this.http.get<SessionUser>(`${environment.apiBaseUrl}/users/me`).pipe(
      tap((user) => this.setSession(this.tokenSignal(), user)),
      catchError(() => {
        this.clearSession();
        return of(null);
      }),
    );
  }

  hasRole(roles: Role[]): boolean {
    const user = this.userSignal();
    if (!user) {
      return false;
    }
    return roles.includes(user.role);
  }

  logout(redirect = true): void {
    this.clearSession();
    if (redirect) {
      void this.router.navigate(['/login']);
    }
  }

  private hydrateSession(): void {
    const token = localStorage.getItem(TOKEN_KEY);
    const userRaw = localStorage.getItem(USER_KEY);

    if (!token || !userRaw) {
      return;
    }

    try {
      const user = JSON.parse(userRaw) as SessionUser;
      this.setSession(token, user);
    } catch {
      this.clearSession();
    }
  }

  private setSession(token: string | null, user: SessionUser): void {
    if (!token) {
      return;
    }

    this.tokenSignal.set(token);
    this.userSignal.set(user);

    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  private clearSession(): void {
    this.tokenSignal.set(null);
    this.userSignal.set(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }
}
