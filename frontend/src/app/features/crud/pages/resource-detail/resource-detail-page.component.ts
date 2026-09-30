import { NgFor, NgIf, NgSwitch, NgSwitchCase, NgSwitchDefault } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { combineLatest, distinctUntilChanged, map } from 'rxjs';

import { ResourceCrudService } from '../../services/resource-crud.service';
import { ResourceDefinition } from '../../../../core/models/resource.models';
import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge.component';
import { toImageSrc } from '../../../../core/utils/image.util';
import { optionLabel, relationLabel } from '../../../../core/utils/relation.util';

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
    // route.data emite dos veces al entrar a una ruta con parametros: sin distinctUntilChanged cada
    // visita al detalle pedia el mismo registro dos veces.
    combineLatest([this.route.data, this.route.paramMap])
      .pipe(
        map(([data, params]) => ({ resourceKey: String(data['resourceKey'] ?? ''), id: params.get('id') })),
        distinctUntilChanged((a, b) => a.resourceKey === b.resourceKey && a.id === b.id),
        takeUntilDestroyed(),
      )
      .subscribe(({ resourceKey, id }) => {
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

    // El nombre de la relación va primero: el backend manda `categoryId` Y `category`, y leer el id
    // directo mostraba el UUID al usuario.
    const label = relationLabel(row, key);
    if (label) {
      return label;
    }

    const direct = row[key];
    if (direct === undefined || direct === null || direct === '') {
      return '-';
    }

    return optionLabel(this.definition()?.fields.find((field) => field.key === key)?.options, direct) ?? direct;
  }

  imageSrc(key: string): string | null {
    return toImageSrc(this.entity()?.[key]);
  }
}
