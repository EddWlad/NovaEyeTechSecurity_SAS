import { NgFor, NgIf, NgSwitch, NgSwitchCase } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable, catchError, forkJoin, map, of, switchMap } from 'rxjs';

import { ResourceCrudService } from '../../services/resource-crud.service';
import { LookupService } from '../../services/lookup.service';
import { ResourceDefinition, ResourceField } from '../../../../core/models/resource.models';
import { NotificationService } from '../../../../core/services/notification.service';
import { toImageSrc } from '../../../../core/utils/image.util';
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

  /** Imagen elegida y aún sin subir: se envía a Cloudinary después de guardar el producto. */
  private pendingProductImage: File | null = null;
  /** El producto ya tenía una imagen guardada, o sea que "quitar" implica borrarla. */
  private hadProductImage = false;

  readonly form = this.fb.group({});
  readonly options = signal<Record<string, { label: string; value: string | boolean }[]>>({});
  readonly isProductsModule = computed(() => this.definition()?.key === 'products');

  constructor() {
    inject(DestroyRef).onDestroy(() => this.revokeProductPreview());

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
      this.revokeProductPreview();
      this.pendingProductImage = null;
      this.hadProductImage = false;
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
          const savedImage = toImageSrc(patch['imageUrl']);
          this.hadProductImage = !!savedImage;
          this.productImagePreview.set(savedImage);
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

      // La imagen se sube aparte y nunca viaja en el cuerpo: reenviar la heredada (base64) al editar
      // haría que el backend la rechace, y las nuevas van a Cloudinary.
      if (field.key === 'imageUrl' && this.isProductsModule()) {
        return;
      }

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

      if (value === '') {
        return;
      }

      payload[field.key] = value;
    });

    this.saving.set(true);

    const request$ = this.isEditMode
      ? this.crudService.update(definition.key, this.entityId()!, payload)
      : this.crudService.create(definition.key, payload);

    request$.pipe(switchMap((response) => this.syncProductImage(response))).subscribe({
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
      // Sin esto un error dejaba el botón en "Guardando..." y deshabilitado hasta recargar la página.
      error: () => this.saving.set(false),
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
      .then(({ optimized, previewUrl }) => {
        this.revokeProductPreview();
        this.pendingProductImage = optimized;
        this.productImagePreview.set(previewUrl);
        this.selectedProductImageName.set(file.name);
        this.productImageRemoved.set(false);
      })
      .catch(() => {
        this.notifications.error('No se pudo procesar la imagen.');
        input.value = '';
      });
  }

  clearProductImage(input: HTMLInputElement): void {
    this.revokeProductPreview();
    this.pendingProductImage = null;
    this.productImagePreview.set(null);
    this.selectedProductImageName.set('');
    // Solo hay algo que borrar del almacenamiento si el producto ya tenía una imagen guardada.
    this.productImageRemoved.set(this.hadProductImage);
    input.value = '';
  }

  /**
   * Sincroniza la imagen con el almacenamiento una vez guardado el producto (el id ya existe).
   * Si la imagen falla, el producto queda guardado y se avisa: no se pierde lo que el usuario escribió.
   */
  private syncProductImage(response: unknown): Observable<unknown> {
    const definition = this.definition();
    const id = (response as { id?: string } | null)?.id;

    if (!definition || !this.isProductsModule() || !id) {
      return of(response);
    }

    let image$: Observable<unknown> | null = null;
    if (this.pendingProductImage) {
      image$ = this.crudService.uploadImage(definition.key, id, this.pendingProductImage);
    } else if (this.productImageRemoved() && this.hadProductImage) {
      image$ = this.crudService.removeImage(definition.key, id);
    }

    if (!image$) {
      return of(response);
    }

    return image$.pipe(
      map(() => response),
      catchError(() => {
        this.notifications.error(
          'El producto se guardó, pero la imagen no se pudo actualizar. Edítalo para reintentar.',
        );
        return of(response);
      }),
    );
  }

  private revokeProductPreview(): void {
    const preview = this.productImagePreview();
    if (preview?.startsWith('blob:')) {
      URL.revokeObjectURL(preview);
    }
  }

  /** Reduce la imagen a 920 px y la convierte a JPEG antes de subirla; la vista previa es local. */
  private buildOptimizedProductImage(file: File): Promise<{ optimized: File; previewUrl: string }> {
    return new Promise((resolve, reject) => {
      const source = URL.createObjectURL(file);
      const image = new Image();

      image.onerror = () => {
        URL.revokeObjectURL(source);
        reject(new Error('No se pudo cargar imagen'));
      };
      image.onload = () => {
        URL.revokeObjectURL(source);

        const maxSide = 920;
        const ratio = Math.min(maxSide / image.width, maxSide / image.height, 1);
        const canvas = document.createElement('canvas');
        canvas.width = Math.round(image.width * ratio);
        canvas.height = Math.round(image.height * ratio);

        const ctx = canvas.getContext('2d');
        if (!ctx) {
          reject(new Error('No se pudo procesar imagen'));
          return;
        }

        ctx.drawImage(image, 0, 0, canvas.width, canvas.height);
        canvas.toBlob(
          (blob) => {
            if (!blob) {
              reject(new Error('No se pudo procesar imagen'));
              return;
            }

            const baseName = file.name.replace(/\.[^.]+$/, '') || 'producto';
            const optimized = new File([blob], `${baseName}.jpg`, { type: 'image/jpeg' });
            resolve({ optimized, previewUrl: URL.createObjectURL(optimized) });
          },
          'image/jpeg',
          0.84,
        );
      };

      image.src = source;
    });
  }
}
