import { NgClass, NgFor, NgIf } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

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
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => {
        const rawChunks = event.urlAfterRedirects
          .split('?')[0]
          .split('/')
          .filter(Boolean);

        const chunks = rawChunks.map((chunk) => this.toLabel(chunk));

        if (rawChunks[1] && this.looksLikeUuid(rawChunks[1])) {
          if (rawChunks[0] === 'users') {
            // Never expose UUID in breadcrumbs for user detail/edit.
            chunks[1] = 'Usuario';
            this.crumbs.set(['Inicio', ...chunks]);

            this.api.get<{ fullName: string }>('users', rawChunks[1]).subscribe({
              next: (user) => {
                const updated = [...chunks];
                updated[1] = user.fullName || 'Usuario';
                this.crumbs.set(['Inicio', ...updated]);
              },
            });
          } else if (rawChunks[0] === 'clients') {
            // Never expose UUID in breadcrumbs for client detail/edit.
            chunks[1] = 'Cliente';
            this.crumbs.set(['Inicio', ...chunks]);

            this.api.get<{ nameOrBusinessName: string }>('clients', rawChunks[1]).subscribe({
              next: (client) => {
                const updated = [...chunks];
                updated[1] = client.nameOrBusinessName || 'Cliente';
                this.crumbs.set(['Inicio', ...updated]);
              },
            });
          } else if (rawChunks[0] === 'suppliers') {
            // Never expose UUID in breadcrumbs for supplier detail/edit.
            chunks[1] = 'Proveedor';
            this.crumbs.set(['Inicio', ...chunks]);

            this.api.get<{ businessName: string }>('suppliers', rawChunks[1]).subscribe({
              next: (supplier) => {
                const updated = [...chunks];
                updated[1] = supplier.businessName || 'Proveedor';
                this.crumbs.set(['Inicio', ...updated]);
              },
            });
          } else if (rawChunks[0] === 'product-categories') {
            // Never expose UUID in breadcrumbs for product category detail/edit.
            chunks[1] = 'Categoría';
            this.crumbs.set(['Inicio', ...chunks]);

            this.api.get<{ name: string }>('product-categories', rawChunks[1]).subscribe({
              next: (category) => {
                const updated = [...chunks];
                updated[1] = category.name || 'Categoría';
                this.crumbs.set(['Inicio', ...updated]);
              },
            });
          } else if (rawChunks[0] === 'products') {
            // Never expose UUID in breadcrumbs for product detail/edit.
            chunks[1] = 'Producto';
            this.crumbs.set(['Inicio', ...chunks]);

            this.api.get<{ name: string }>('products', rawChunks[1]).subscribe({
              next: (product) => {
                const updated = [...chunks];
                updated[1] = product.name || 'Producto';
                this.crumbs.set(['Inicio', ...updated]);
              },
            });
          } else if (rawChunks[0] === 'service-categories') {
            // Never expose UUID in breadcrumbs for service category detail/edit.
            chunks[1] = 'Categoría';
            this.crumbs.set(['Inicio', ...chunks]);

            this.api.get<{ name: string }>('service-categories', rawChunks[1]).subscribe({
              next: (category) => {
                const updated = [...chunks];
                updated[1] = category.name || 'Categoría';
                this.crumbs.set(['Inicio', ...updated]);
              },
            });
          } else if (rawChunks[0] === 'services') {
            // Never expose UUID in breadcrumbs for service detail/edit.
            chunks[1] = 'Servicio';
            this.crumbs.set(['Inicio', ...chunks]);

            this.api.get<{ name: string }>('services', rawChunks[1]).subscribe({
              next: (service) => {
                const updated = [...chunks];
                updated[1] = service.name || 'Servicio';
                this.crumbs.set(['Inicio', ...updated]);
              },
            });
          } else if (rawChunks[0] === 'maintenance') {
            // Never expose UUID in breadcrumbs for maintenance detail/edit.
            chunks[1] = 'Mantenimiento';
            this.crumbs.set(['Inicio', ...chunks]);

            this.api
              .get<{ intervenedSystem?: string; client?: { nameOrBusinessName?: string } }>(
                'maintenance',
                rawChunks[1],
              )
              .subscribe({
                next: (maintenance) => {
                  const updated = [...chunks];
                  updated[1] =
                    maintenance.intervenedSystem ||
                    maintenance.client?.nameOrBusinessName ||
                    'Mantenimiento';
                  this.crumbs.set(['Inicio', ...updated]);
                },
              });
          } else if (rawChunks[0] === 'quotations') {
            // Never expose UUID in breadcrumbs for quotation detail/edit/preview.
            chunks[1] = 'Cotización';
            this.crumbs.set(['Inicio', ...chunks]);

            this.api
              .get<{ quotationNumber?: string }>('quotations', rawChunks[1])
              .subscribe({
                next: (quotation) => {
                  const updated = [...chunks];
                  updated[1] = quotation.quotationNumber || 'Cotización';
                  this.crumbs.set(['Inicio', ...updated]);
                },
              });
          } else {
            this.crumbs.set(['Inicio', ...chunks]);
          }
        } else {
          this.crumbs.set(['Inicio', ...chunks]);
        }

        this.sidebarOpen.set(false);
      });
  }

  private toLabel(chunk: string): string {
    return chunk
      .replaceAll('-', ' ')
      .replace(/^./, (letter) => letter.toUpperCase());
  }

  private looksLikeUuid(value: string): boolean {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(
      value,
    );
  }

  toggleSidebar(): void {
    this.sidebarOpen.update((value) => !value);
  }

  logout(): void {
    this.authService.logout();
  }
}
