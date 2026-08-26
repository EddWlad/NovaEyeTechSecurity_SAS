import { NgIf } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AuthService } from '../../../../core/services/auth.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';
import { QuotationSettingsService } from '../../services/quotation-settings.service';

@Component({
  selector: 'app-quotation-settings-page',
  standalone: true,
  imports: [NgIf, ReactiveFormsModule, PageHeaderComponent, LoadingSpinnerComponent],
  templateUrl: './quotation-settings-page.component.html',
})
export class QuotationSettingsPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly settingsService = inject(QuotationSettingsService);
  private readonly notifications = inject(NotificationService);
  private readonly authService = inject(AuthService);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly canEdit = computed(() => this.authService.hasRole(['ADMINISTRADOR']));

  readonly form = this.fb.group({
    currentVat: ['15', [Validators.required]],
    allowedVatRates: ['0,1,12,15', [Validators.required]],
    allowedMargins: ['0,10,12,20,25,30', [Validators.required]],
    defaultMargin: ['20', [Validators.required]],
    defaultCurrency: ['USD', [Validators.required]],
    defaultValidityDays: [15, [Validators.required, Validators.min(1)]],
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.settingsService.getCurrent().subscribe({
      next: (settings) => {
        this.form.patchValue({
          currentVat: this.normalizePercentToken(settings.currentVat, '15'),
          allowedVatRates: settings.allowedVatRates
            .map((value) => this.normalizePercentToken(value, '15'))
            .join(','),
          allowedMargins: settings.allowedMargins
            .map((value) => this.normalizePercentToken(value, '20'))
            .join(','),
          defaultMargin: this.normalizePercentToken(settings.defaultMargin, '20'),
          defaultCurrency: settings.defaultCurrency,
          defaultValidityDays: settings.defaultValidityDays,
        });
      },
      complete: () => this.loading.set(false),
    });
  }

  submit(): void {
    if (!this.canEdit()) {
      this.notifications.info('Solo el administrador puede modificar esta pantalla.');
      return;
    }

    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const payload = {
      currentVat: this.normalizePercentToken(value.currentVat, '15'),
      allowedVatRates: String(value.allowedVatRates ?? '')
        .split(',')
        .map((item) => this.normalizePercentToken(item, '15'))
        .filter(Boolean),
      allowedMargins: String(value.allowedMargins ?? '')
        .split(',')
        .map((item) => this.normalizePercentToken(item, '20'))
        .filter(Boolean),
      defaultMargin: this.normalizePercentToken(value.defaultMargin, '20'),
      defaultCurrency: String(value.defaultCurrency ?? 'USD').trim().toUpperCase(),
      defaultValidityDays: Number(value.defaultValidityDays ?? 15),
    };

    this.saving.set(true);

    this.settingsService.update(payload).subscribe({
      next: () => {
        this.notifications.success('Parámetros actualizados correctamente.');
      },
      complete: () => this.saving.set(false),
    });
  }

  private normalizePercentToken(value: unknown, fallback: string): string {
    const raw = String(value ?? '')
      .trim()
      .replace(',', '.');
    const parsed = Number(raw);

    if (!Number.isFinite(parsed)) {
      return fallback;
    }

    return String(Math.round(parsed));
  }
}
