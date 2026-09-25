import { NgClass, NgFor, NgIf } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Observable, catchError, filter, map, of, startWith, switchMap, tap } from 'rxjs';

import { ENTITY_CRUMBS, looksLikeUuid, segmentLabel } from '../config/breadcrumb.config';
import { NAVIGATION_ITEMS } from '../config/navigation.config';
import { AuthService } from '../services/auth.service';
import { ApiService } from '../services/api.service';
import { ROLE_LABELS } from '../../shared/constants/roles.constants';
import { BreadcrumbsComponent } from './breadcrumbs.component';
import { ToastStackComponent } from '../../shared/components/toast-stack.component';
import { toInitials } from '../utils/format.util';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    NgClass,
    NgFor,
    NgIf,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    BreadcrumbsComponent,
    ToastStackComponent,
  ],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss',
})
export class AppShellComponent {
  private readonly router = inject(Router);
  private readonly api = inject(ApiService);
  readonly authService = inject(AuthService);

  readonly sidebarOpen = signal(false);
  readonly crumbs = signal<string[]>(['Inicio']);

  readonly menuItems = computed(() => {
    const user = this.authService.user();
    if (!user) {
      return [];
    }
    return NAVIGATION_ITEMS.filter((item) => item.roles.includes(user.role));
  });

  readonly currentRoleLabel = computed(() => {
    const role = this.authService.user()?.role;
    return role ? ROLE_LABELS[role] : '';
  });

  readonly userInitials = computed(() => toInitials(this.authService.user()?.fullName ?? '')); 

  constructor() {
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        tap(() => this.sidebarOpen.set(false)),
        // switchMap descarta la respuesta de la navegacion anterior: antes una respuesta tardia
        // pisaba las migas de la pantalla actual.
        switchMap((event) => this.breadcrumbsFor(event.urlAfterRedirects)),
        takeUntilDestroyed(),
      )
      .subscribe((crumbs) => this.crumbs.set(crumbs));
  }

  /**
   * Migas de una URL. Si el segundo segmento es un id emite primero con el nombre generico
   * ("Producto") y luego con el nombre real del registro, sin mostrar nunca el UUID.
   */
  private breadcrumbsFor(url: string): Observable<string[]> {
    const segments = url.split('?')[0].split('/').filter(Boolean);
    const [resource, id] = segments;
    const entity = resource ? ENTITY_CRUMBS[resource] : undefined;
    const labels = segments.map(segmentLabel);

    if (!entity || !id || !looksLikeUuid(id)) {
      return of(['Inicio', ...labels]);
    }

    const withLeaf = (leaf: string) => ['Inicio', labels[0], leaf, ...labels.slice(2)];

    return this.api.get<Record<string, unknown>>(resource, id).pipe(
      map((record) => withLeaf(entity.label(record) ?? entity.fallback)),
      catchError(() => of(withLeaf(entity.fallback))),
      startWith(withLeaf(entity.fallback)),
    );
  }

  toggleSidebar(): void {
    this.sidebarOpen.update((value) => !value);
  }

  logout(): void {
    this.authService.logout();
  }
}
