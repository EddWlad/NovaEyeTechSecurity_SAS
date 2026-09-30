import { NgFor, NgIf, NgSwitch, NgSwitchCase, NgSwitchDefault } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state.component';
import { PaginationControlsComponent } from '../../../../shared/components/pagination-controls.component';
import { ResourceCrudService } from '../../services/resource-crud.service';
import { ResourceDefinition } from '../../../../core/models/resource.models';
import { AuthService } from '../../../../core/services/auth.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { toImageSrc } from '../../../../core/utils/image.util';
import { optionLabel, relationLabel } from '../../../../core/utils/relation.util';
import { onSearchChange } from '../../../../core/utils/search.util';
import { MANAGER_ROLES } from '../../../../shared/constants/roles.constants';

@Component({
  selector: 'app-resource-list-page',
  standalone: true,
  imports: [
    NgFor,
    NgIf,
    NgSwitch,
    NgSwitchCase,
    NgSwitchDefault,
    FormsModule,
    RouterLink,
    PageHeaderComponent,
    StatusBadgeComponent,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    PaginationControlsComponent,
  ],
  templateUrl: './resource-list-page.component.html',
  styleUrl: './resource-list-page.component.scss',
})
export class ResourceListPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly crudService = inject(ResourceCrudService);
  private readonly authService = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  readonly definition = signal<ResourceDefinition | null>(null);
  readonly loading = signal(true);
  readonly rows = signal<Record<string, unknown>[]>([]);
  readonly query = signal('');
  readonly page = signal(1);
  readonly limit = signal(10);
  readonly total = signal(0);
  readonly totalPages = signal(1);
  readonly showDeleteModal = signal(false);
  readonly pendingDeleteId = signal<string | null>(null);
  readonly pendingDeleteLabel = signal('este registro');

  readonly columns = computed(() => {
    const def = this.definition();
    return (def?.fields ?? []).filter((field) => !field.hideInList && field.key !== 'password');
  });

  private loadSubscription?: Subscription;

  readonly canWrite = computed(() => {
    const def = this.definition();
    if (!def) {
      return false;
    }
    if (def.key === 'clients') {
      return true;
    }
    return this.authService.hasRole(MANAGER_ROLES);
  });

  readonly usesDeleteModal = computed(() => {
    const key = this.definition()?.key;
    return key === 'users' || key === 'clients' || key === 'suppliers' || key === 'product-categories' || key === 'products' || key === 'service-categories' || key === 'services';
  });

  constructor() {
    // La busqueda la resuelve el servidor sobre todos los registros; antes solo filtraba la pagina visible.
    onSearchChange(this.query, () => this.load(1));

    this.route.data.subscribe((data) => {
      const resourceKey = String(data['resourceKey'] ?? '');
      const definition = this.crudService.getDefinition(resourceKey);
      this.definition.set(definition);
      this.query.set('');
      this.page.set(1);
      this.load(1);
    });
  }

  load(nextPage = this.page()): void {
    const def = this.definition();
    if (!def) {
      return;
    }

    this.loading.set(true);
    // Una respuesta lenta de una busqueda anterior no debe pisar la actual.
    this.loadSubscription?.unsubscribe();
    this.loadSubscription = this.crudService
      .listPaginated<Record<string, unknown>>(def.key, nextPage, this.limit(), this.query())
      .subscribe({
      next: (response) => {
        this.rows.set(response.items);
        this.page.set(response.page);
        this.limit.set(response.limit);
        this.total.set(response.total);
        this.totalPages.set(response.totalPages);
      },
      error: () => this.loading.set(false),
      complete: () => this.loading.set(false),
    });
  }

  changePage(nextPage: number): void {
    this.load(nextPage);
  }

  view(id: string): void {
    const def = this.definition();
    if (!def) {
      return;
    }
    void this.router.navigate([`/${def.key}/${id}`]);
  }

  edit(id: string): void {
    const def = this.definition();
    if (!def) {
      return;
    }
    void this.router.navigate([`/${def.key}/${id}/edit`]);
  }

  remove(id: string): void {
    const def = this.definition();
    if (!def || !this.canWrite()) {
      return;
    }

    if (this.usesDeleteModal()) {
      const row = this.rows().find((item) => this.asId(item['id']) === id);
      this.pendingDeleteLabel.set(this.getDeleteLabel(def.key, row ?? {}));
      this.pendingDeleteId.set(id);
      this.showDeleteModal.set(true);
      return;
    }

    if (!window.confirm(`¿Eliminar ${def.singular}? Esta acción no se puede deshacer.`)) {
      return;
    }

    this.executeDelete(id);
  }

  closeDeleteModal(): void {
    this.showDeleteModal.set(false);
    this.pendingDeleteId.set(null);
  }

  confirmDelete(): void {
    const id = this.pendingDeleteId();
    if (!id) {
      return;
    }
    this.closeDeleteModal();
    this.executeDelete(id);
  }

  private executeDelete(id: string): void {
    const def = this.definition();
    if (!def) {
      return;
    }

    this.crudService.delete(def.key, id).subscribe({
      next: () => {
        this.notifications.success(`${def.singular} eliminado correctamente.`);
        const shouldGoPrevious =
          this.rows().length === 1 && this.page() > 1;
        this.load(shouldGoPrevious ? this.page() - 1 : this.page());
      },
    });
  }

  private getDeleteLabel(resourceKey: string, row: Record<string, unknown>): string {
    if (resourceKey === 'users') {
      const fullName = this.getCellValue(row, 'fullName');
      return fullName !== '-' ? fullName : 'este usuario';
    }

    if (resourceKey === 'clients') {
      const clientName = this.getCellValue(row, 'nameOrBusinessName');
      return clientName !== '-' ? clientName : 'este cliente';
    }

    if (resourceKey === 'suppliers') {
      const businessName = this.getCellValue(row, 'businessName');
      return businessName !== '-' ? businessName : 'este proveedor';
    }

    if (resourceKey === 'product-categories') {
      const categoryName = this.getCellValue(row, 'name');
      return categoryName !== '-' ? categoryName : 'esta categoría de producto';
    }

    if (resourceKey === 'products') {
      const productName = this.getCellValue(row, 'name');
      return productName !== '-' ? productName : 'este producto';
    }

    if (resourceKey === 'service-categories') {
      const categoryName = this.getCellValue(row, 'name');
      return categoryName !== '-' ? categoryName : 'esta categoría de servicio';
    }

    if (resourceKey === 'services') {
      const serviceName = this.getCellValue(row, 'name');
      return serviceName !== '-' ? serviceName : 'este servicio';
    }

    return 'este registro';
  }

  getCellValue(row: Record<string, unknown>, key: string): string {
    if (key.endsWith('Id')) {
      return relationLabel(row, key) ?? '-';
    }

    const value = row[key];

    if (key === 'active') {
      return String(value);
    }

    if (value === null || value === undefined || value === '') {
      return '-';
    }

    const option = optionLabel(this.definition()?.fields.find((field) => field.key === key)?.options, value);
    if (option) {
      return option;
    }

    if (typeof value === 'object' && value !== null) {
      const obj = value as Record<string, unknown>;
      return String(obj['name'] ?? obj['fullName'] ?? obj['businessName'] ?? obj['email'] ?? '-');
    }

    return String(value);
  }

  productImageSrc(row: Record<string, unknown>): string | null {
    return toImageSrc(row['imageUrl']);
  }

  asId(value: unknown): string {
    return String(value ?? '');
  }
}
