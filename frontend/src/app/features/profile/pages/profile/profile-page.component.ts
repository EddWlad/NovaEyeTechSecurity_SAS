import { NgIf } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, catchError, map, of, switchMap } from 'rxjs';

import { AuthService } from '../../../../core/services/auth.service';
import { ApiService } from '../../../../core/services/api.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { ROLE_LABELS } from '../../../../shared/constants/roles.constants';
import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';
import { toInitials } from '../../../../core/utils/format.util';
import { toImageSrc } from '../../../../core/utils/image.util';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [NgIf, ReactiveFormsModule, PageHeaderComponent, LoadingSpinnerComponent],
  templateUrl: './profile-page.component.html',
  styleUrl: './profile-page.component.scss',
})
export class ProfilePageComponent {
  private readonly fb = inject(FormBuilder);
  readonly authService = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly notifications = inject(NotificationService);

  readonly loading = signal(true);
  readonly saving = signal(false);
  /** Foto que se muestra: la guardada (URL de Cloudinary) o la vista previa local de la elegida. */
  readonly avatarDataUrl = signal<string | null>(null);
  readonly selectedFileName = signal('');

  /** Foto elegida y aún sin subir: se envía a Cloudinary al guardar el perfil. */
  private pendingAvatar: File | null = null;
  /** El usuario ya tenía foto guardada, o sea que "quitar" implica borrarla. */
  private hadAvatar = false;
  private avatarRemoved = false;

  readonly form = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(3)]],
    email: [{ value: '', disabled: true }, [Validators.required, Validators.email]],
    phone: [''],
  });

  readonly roleLabel = signal('');

  constructor() {
    inject(DestroyRef).onDestroy(() => this.revokePreview());
    this.load();
  }

  load(): void {
    this.loading.set(true);

    this.authService.refreshProfile().subscribe({
      next: (user) => {
        if (!user) {
          return;
        }

        this.form.patchValue({
          fullName: user.fullName,
          email: user.email,
          phone: user.phone ?? '',
        });

        this.revokePreview();
        const savedAvatar = toImageSrc(user.avatarDataUrl);
        this.avatarDataUrl.set(savedAvatar);
        this.hadAvatar = !!savedAvatar;
        this.pendingAvatar = null;
        this.avatarRemoved = false;
        this.selectedFileName.set('');

        this.roleLabel.set(ROLE_LABELS[user.role]);
      },
      complete: () => this.loading.set(false),
    });
  }

  submit(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);

    // La foto ya no viaja en el perfil: se sube (o se quita) aparte, después de guardar los datos.
    const payload = {
      fullName: this.form.controls.fullName.value ?? '',
      phone: this.form.controls.phone.value ?? '',
    };

    this.api
      .patch('users/me/profile', payload)
      .pipe(switchMap(() => this.syncAvatar()))
      .subscribe({
        next: () => {
          this.notifications.success('Perfil actualizado correctamente.');
          this.load();
        },
        complete: () => this.saving.set(false),
        error: () => this.saving.set(false),
      });
  }

  onAvatarSelected(event: Event): void {
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

    this.buildOptimizedAvatar(file)
      .then(({ optimized, previewUrl }) => {
        this.revokePreview();
        this.pendingAvatar = optimized;
        this.avatarRemoved = false;
        this.avatarDataUrl.set(previewUrl);
        this.selectedFileName.set(file.name);
      })
      .catch(() => {
        this.notifications.error('No se pudo procesar la imagen.');
        input.value = '';
      });
  }

  clearAvatar(input: HTMLInputElement): void {
    this.revokePreview();
    this.pendingAvatar = null;
    this.avatarDataUrl.set(null);
    this.selectedFileName.set('');
    // Solo hay algo que borrar del almacenamiento si el usuario ya tenía una foto guardada.
    this.avatarRemoved = this.hadAvatar;
    input.value = '';
  }

  initials(name: string): string {
    return toInitials(name || 'Perfil');
  }

  /** Sube o quita la foto según lo que el usuario decidió. Un fallo no deshace el perfil ya guardado. */
  private syncAvatar(): Observable<unknown> {
    let avatar$: Observable<unknown> | null = null;

    if (this.pendingAvatar) {
      const body = new FormData();
      body.append('file', this.pendingAvatar);
      avatar$ = this.api.post('users/me/avatar', body);
    } else if (this.avatarRemoved && this.hadAvatar) {
      avatar$ = this.api.remove('users/me/avatar');
    }

    if (!avatar$) {
      return of(null);
    }

    return avatar$.pipe(
      map(() => null),
      catchError(() => {
        this.notifications.error('Se guardaron tus datos, pero la foto no se pudo actualizar.');
        return of(null);
      }),
    );
  }

  private revokePreview(): void {
    const preview = this.avatarDataUrl();
    if (preview?.startsWith('blob:')) {
      URL.revokeObjectURL(preview);
    }
  }

  /** Recorta al centro en un cuadrado de 240 px y lo convierte a JPEG; la vista previa es local. */
  private buildOptimizedAvatar(file: File): Promise<{ optimized: File; previewUrl: string }> {
    return new Promise((resolve, reject) => {
      const source = URL.createObjectURL(file);
      const image = new Image();

      image.onerror = () => {
        URL.revokeObjectURL(source);
        reject(new Error('No se pudo cargar imagen'));
      };
      image.onload = () => {
        URL.revokeObjectURL(source);

        const size = 240;
        const canvas = document.createElement('canvas');
        canvas.width = size;
        canvas.height = size;

        const ctx = canvas.getContext('2d');
        if (!ctx) {
          reject(new Error('No se pudo procesar imagen'));
          return;
        }

        // Crop cuadrado centrado para mantener avatar consistente.
        const minSide = Math.min(image.width, image.height);
        const sx = (image.width - minSide) / 2;
        const sy = (image.height - minSide) / 2;
        ctx.drawImage(image, sx, sy, minSide, minSide, 0, 0, size, size);

        canvas.toBlob(
          (blob) => {
            if (!blob) {
              reject(new Error('No se pudo procesar imagen'));
              return;
            }

            const optimized = new File([blob], 'avatar.jpg', { type: 'image/jpeg' });
            resolve({ optimized, previewUrl: URL.createObjectURL(optimized) });
          },
          'image/jpeg',
          0.82,
        );
      };

      image.src = source;
    });
  }
}
