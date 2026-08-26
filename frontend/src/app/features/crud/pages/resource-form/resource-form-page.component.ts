import { NgFor, NgIf, NgSwitch, NgSwitchCase } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';

import { ResourceCrudService } from '../../services/resource-crud.service';
import { LookupService } from '../../services/lookup.service';
import { ResourceDefinition, ResourceField } from '../../../../core/models/resource.models';
import { NotificationService } from '../../../../core/services/notification.service';
import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';

@Component({
  selector: 'app-resource-form-page',
  standalone: true,
  imports: [
    NgFor,
    NgIf,
    NgSwitch,
    NgSwitchCase,
    RouterLink,
    ReactiveFormsModule,
    PageHeaderComponent,
    LoadingSpinnerComponent,
  ],
  templateUrl: './resource-form-page.component.html',
  styleUrl: './resource-form-page.component.scss',
})
export class ResourceFormPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly crudService = inject(ResourceCrudService);
  private readonly lookupService = inject(LookupService);
  private readonly notifications = inject(NotificationService);

  readonly definition = signal<ResourceDefinition | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly entityId = signal<string | null>(null);
  readonly productImagePreview = signal<string | null>(null);
  readonly selectedProductImageName = signal('');
  readonly productImageRemoved = signal(false);

  readonly form = this.fb.group({});
  readonly options = signal<Record<string, { label: string; value: string | boolean }[]>>({});
  readonly isProductsModule = computed(() => this.definition()?.key === 'products');

  constructor() {
    this.route.data.subscribe((data) => {
      const resourceKey = String(data['resourceKey'] ?? '');
      this.definition.set(this.crudService.getDefinition(resourceKey));
      this.entityId.set(this.route.snapshot.paramMap.get('id'));
      this.setupForm();
      this.loadLookupsAndEntity();
    });
  }

  get isEditMode(): boolean {
    return !!this.entityId();
  }

  private setupForm(): void {
    const definition = this.definition();
    if (!definition) {
      return;
    }

    const controls: Record<string, unknown> = {};

    definition.fields.forEach((field) => {
      const validators = [];

      if (field.required || (field.requiredOnCreateOnly && !this.isEditMode)) {
        validators.push(Validators.required);
      }

      if (field.type === 'email') {
        validators.push(Validators.email);
      }

      controls[field.key] = this.fb.control(field.type === 'boolean' ? true : '', validators);
    });

    this.form.reset();
    Object.entries(controls).forEach(([key, control]) => {
      this.form.addControl(key, control as never);
    });

    if (this.isProductsModule()) {
      this.productImagePreview.set(null);
      this.selectedProductImageName.set('');
      this.productImageRemoved.set(false);
    }
  }

  private loadLookupsAndEntity(): void {
    const definition = this.definition();
    if (!definition) {
      return;
    }

    this.loading.set(true);

    const lookupRequests = definition.fields
      .filter((field) => field.type === 'select' && field.endpoint)
      .map((field) => this.lookupService.list(field.endpoint!).pipe());

    const lookupStream = lookupRequests.length ? forkJoin(lookupRequests) : of([]);

    lookupStream.subscribe({
      next: (responses) => {
        const map: Record<string, { label: string; value: string | boolean }[]> = {};
        let responseIndex = 0;

        definition.fields.forEach((field) => {
          if (field.type === 'select') {
            if (field.options?.length) {
              map[field.key] = field.options;
            } else if (field.endpoint) {
              const rows = responses[responseIndex++] as Record<string, unknown>[];
              map[field.key] = rows.map((row) => ({
                label: String(row[field.optionLabelKey ?? 'name'] ?? row['name'] ?? row['fullName'] ?? row['businessName']),
                value: String(row[field.optionValueKey ?? 'id'] ?? row['id']),
              }));
            }
          }
        });

        this.options.set(map);

        if (this.isEditMode) {
          this.loadEntity();
        } else {
          this.loading.set(false);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  private loadEntity(): void {
    const definition = this.definition();
    const id = this.entityId();
    if (!definition || !id) {
      this.loading.set(false);
      return;
    }

    this.crudService.get<Record<string, unknown>>(definition.key, id).subscribe({
      next: (entity) => {
        const patch: Record<string, unknown> = {};
        definition.fields.forEach((field) => {
          patch[field.key] = this.resolveFieldValue(entity, field);
        });
        this.form.patchValue(patch);

        if (this.isProductsModule()) {
          const imageUrl = String(patch['imageUrl'] ?? '').trim();
          this.productImagePreview.set(this.normalizeProductImageUrl(imageUrl || null));
          this.selectedProductImageName.set('');
          this.productImageRemoved.set(false);
        }
      },
      complete: () => this.loading.set(false),
    });
  }

  private resolveFieldValue(entity: Record<string, unknown>, field: ResourceField): unknown {
    const directValue = entity[field.key];
    if (directValue !== undefined) {
      return directValue;
    }

    if (field.key.endsWith('Id')) {
      const relationName = field.key.replace(/Id$/, '');
      const relation = entity[relationName] as Record<string, unknown> | undefined;
      return relation?.['id'] ?? '';
    }

    return '';
  }

  submit(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    const definition = this.definition();
    if (!definition) {
      return;
    }

    const raw = this.form.getRawValue() as Record<string, unknown>;
    const payload: Record<string, unknown> = {};

    definition.fields.forEach((field) => {
      let value = raw[field.key];

      if (field.type === 'number' && value !== '') {
        value = String(value);
      }

      if (field.type === 'boolean') {
        value = Boolean(value);
      }

      if (this.isEditMode && field.requiredOnCreateOnly && !value) {
        return;
      }

      if (this.isEditMode && field.key === 'password' && !value) {
        return;
      }

      if (
        field.key === 'imageUrl' &&
        this.isProductsModule() &&
        value === '' &&
        this.productImageRemoved()
      ) {
        payload[field.key] = '';
        return;
      }

      if (value === '') {
        return;
      }

      payload[field.key] = value;
    });

    this.saving.set(true);

    const request$ = this.isEditMode
      ? this.crudService.update(definition.key, this.entityId()!, payload)
      : this.crudService.create(definition.key, payload);

    request$.subscribe({
      next: (response) => {
        const entity = response as { id?: string };
        this.notifications.success(
          this.isEditMode ? `${definition.singular} actualizado correctamente.` : `${definition.singular} creado correctamente.`,
        );

        if (entity.id) {
          void this.router.navigate([`/${definition.key}/${entity.id}`]);
          return;
        }

        void this.router.navigate([`/${definition.key}`]);
      },
      complete: () => this.saving.set(false),
    });
  }

  isInvalid(controlKey: string): boolean {
    const control = this.form.get(controlKey);
    return !!control && control.touched && control.invalid;
  }

  shouldRenderField(field: ResourceField): boolean {
    if (this.isProductsModule() && field.key === 'imageUrl') {
      return false;
    }
    return true;
  }

  onProductImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      return;
    }

    const allowedTypes = ['image/png', 'image/jpeg', 'image/jpg', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
      this.notifications.error('Formato inválido. Usa PNG, JPG o WEBP.');
      input.value = '';
      return;
    }

    const maxSizeBytes = 5 * 1024 * 1024;
    if (file.size > maxSizeBytes) {
      this.notifications.error('La imagen no debe superar 5MB.');
      input.value = '';
      return;
    }

    this.buildOptimizedProductImage(file)
      .then((optimized) => {
        this.form.patchValue({ imageUrl: optimized });
        this.productImagePreview.set(optimized);
        this.selectedProductImageName.set(file.name);
        this.productImageRemoved.set(false);
      })
      .catch(() => {
        this.notifications.error('No se pudo procesar la imagen.');
        input.value = '';
      });
  }

  clearProductImage(input: HTMLInputElement): void {
    this.form.patchValue({ imageUrl: '' });
    this.productImagePreview.set(null);
    this.selectedProductImageName.set('');
    this.productImageRemoved.set(true);
    input.value = '';
  }

  private normalizeProductImageUrl(value: string | null): string | null {
    if (!value) {
      return null;
    }

    if (
      value.startsWith('data:image/') ||
      value.startsWith('http://') ||
      value.startsWith('https://')
    ) {
      return value;
    }

    if (value.startsWith('www.')) {
      return `https://${value}`;
    }

    return value;
  }

  private buildOptimizedProductImage(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onerror = () => reject(new Error('No se pudo leer archivo'));
      reader.onload = () => {
        const image = new Image();
        image.onerror = () => reject(new Error('No se pudo cargar imagen'));
        image.onload = () => {
          const maxSide = 920;
          const ratio = Math.min(maxSide / image.width, maxSide / image.height, 1);
          const width = Math.round(image.width * ratio);
          const height = Math.round(image.height * ratio);

          const canvas = document.createElement('canvas');
          canvas.width = width;
          canvas.height = height;
          const ctx = canvas.getContext('2d');
          if (!ctx) {
            reject(new Error('No se pudo procesar imagen'));
            return;
          }

          ctx.drawImage(image, 0, 0, width, height);
          resolve(canvas.toDataURL('image/jpeg', 0.84));
        };
        image.src = String(reader.result ?? '');
      };
      reader.readAsDataURL(file);
    });
  }
}
