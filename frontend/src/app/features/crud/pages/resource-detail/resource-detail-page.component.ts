import { NgFor, NgIf, NgSwitch, NgSwitchCase, NgSwitchDefault } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ResourceCrudService } from '../../services/resource-crud.service';
import { ResourceDefinition } from '../../../../core/models/resource.models';
import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge.component';

@Component({
  selector: 'app-resource-detail-page',
  standalone: true,
  imports: [
    NgFor,
    NgIf,
    NgSwitch,
    NgSwitchCase,
    NgSwitchDefault,
    RouterLink,
    PageHeaderComponent,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    StatusBadgeComponent,
  ],
  templateUrl: './resource-detail-page.component.html',
  styleUrl: './resource-detail-page.component.scss',
})
export class ResourceDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly crudService = inject(ResourceCrudService);

  readonly definition = signal<ResourceDefinition | null>(null);
  readonly loading = signal(true);
  readonly entity = signal<Record<string, unknown> | null>(null);

  constructor() {
    this.route.data.subscribe((data) => {
      const resourceKey = String(data['resourceKey'] ?? '');
      const id = this.route.snapshot.paramMap.get('id');
      this.definition.set(this.crudService.getDefinition(resourceKey));

      if (!id) {
        this.loading.set(false);
        return;
      }

      this.crudService.get<Record<string, unknown>>(resourceKey, id).subscribe({
        next: (entity) => this.entity.set(entity),
        complete: () => this.loading.set(false),
      });
    });
  }

  valueFor(key: string): unknown {
    if (key === 'password') {
      return '••••••••';
    }

    const row = this.entity();
    if (!row) {
      return '-';
    }

    const direct = row[key];
    if (direct !== undefined) {
      return direct;
    }

    if (key.endsWith('Id')) {
      const relation = row[key.replace(/Id$/, '')] as Record<string, unknown>;
      return relation?.['name'] ?? relation?.['fullName'] ?? relation?.['businessName'] ?? relation?.['id'] ?? '-';
    }

    return '-';
  }
}
