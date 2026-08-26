import { NgIf } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthService } from '../../../../core/services/auth.service';
import { ApiService } from '../../../../core/services/api.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { ROLE_LABELS } from '../../../../shared/constants/roles.constants';
import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';
import { toInitials } from '../../../../core/utils/format.util';

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
  readonly avatarDataUrl = signal<string | null>(null);
  readonly selectedFileName = signal('');

  readonly form = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(3)]],
    email: [{ value: '', disabled: true }, [Validators.required, Validators.email]],
    phone: [''],
  });

  readonly roleLabel = signal('');

  constructor() {
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
        this.avatarDataUrl.set(user.avatarDataUrl ?? null);
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

    const payload = {
      fullName: this.form.controls.fullName.value ?? '',
      phone: this.form.controls.phone.value ?? '',
      avatarDataUrl: this.avatarDataUrl(),
    };

    this.api.patch('users/me/profile', payload).subscribe({
      next: () => {
        this.notifications.success('Perfil actualizado correctamente.');
        this.load();
      },
      complete: () => this.saving.set(false),
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
      .then((optimized) => {
        const approxBytes = this.estimateBytes(optimized);
        if (approxBytes > 95 * 1024) {
          this.notifications.error(
            'La imagen sigue siendo muy pesada. Prueba una foto más ligera.',
          );
          input.value = '';
          return;
        }

        this.avatarDataUrl.set(optimized);
        this.selectedFileName.set(file.name);
      })
      .catch(() => {
        this.notifications.error('No se pudo procesar la imagen.');
        input.value = '';
      });
  }

  clearAvatar(input: HTMLInputElement): void {
    this.avatarDataUrl.set(null);
    this.selectedFileName.set('');
    input.value = '';
  }

  initials(name: string): string {
    return toInitials(name || 'Perfil');
  }

  private buildOptimizedAvatar(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onerror = () => reject(new Error('No se pudo leer archivo'));
      reader.onload = () => {
        const image = new Image();
        image.onerror = () => reject(new Error('No se pudo cargar imagen'));
        image.onload = () => {
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

          resolve(canvas.toDataURL('image/jpeg', 0.82));
        };
        image.src = String(reader.result ?? '');
      };
      reader.readAsDataURL(file);
    });
  }

  private estimateBytes(dataUrl: string): number {
    const base64 = dataUrl.split(',')[1] ?? '';
    return Math.ceil((base64.length * 3) / 4);
  }
}
