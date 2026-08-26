import { NgFor, NgIf } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Client, Product, Quotation, QuotationSetting, Service } from '../../../../core/models/entities.models';
import { QuotationStatus } from '../../../../core/models/enums';
import { ApiService } from '../../../../core/services/api.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { toDateInputValue, toMoney } from '../../../../core/utils/format.util';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { QuotationItemPayload, QuotationsService } from '../../services/quotations.service';

@Component({
  selector: 'app-quotation-form-page',
  standalone: true,
  imports: [
    NgFor,
    NgIf,
    RouterLink,
    ReactiveFormsModule,
    PageHeaderComponent,
    LoadingSpinnerComponent,
  ],
  templateUrl: './quotation-form-page.component.html',
  styleUrl: './quotation-form-page.component.scss',
})
export class QuotationFormPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(ApiService);
  private readonly quotationsService = inject(QuotationsService);
  private readonly notifications = inject(NotificationService);

  readonly quotationId = signal<string | null>(this.route.snapshot.paramMap.get('id'));
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly formVersion = signal(0);

  readonly clients = signal<Client[]>([]);
  readonly products = signal<Product[]>([]);
  readonly services = signal<Service[]>([]);
  readonly settings = signal<QuotationSetting | null>(null);
  readonly existingQuotation = signal<Quotation | null>(null);

  readonly statuses: QuotationStatus[] = ['BORRADOR', 'ENVIADA', 'APROBADA', 'RECHAZADA'];
  readonly isDraftEditable = computed(
    () => this.existingQuotation()?.status === 'BORRADOR',
  );
  readonly showStatusOnlyEditor = computed(
    () => this.isEditMode && !!this.existingQuotation() && !this.isDraftEditable(),
  );
  readonly vatOptions = computed(() =>
    this.normalizeAllowedPercentOptions(this.settings()?.allowedVatRates, [
      '0',
      '1',
      '12',
      '15',
    ]),
  );
  readonly marginOptions = computed(() =>
    this.normalizeAllowedPercentOptions(this.settings()?.allowedMargins, [
      '0',
      '10',
      '12',
      '20',
      '25',
      '30',
    ]),
  );

  readonly form = this.fb.group({
    clientId: ['', [Validators.required]],
    issuedAt: [toDateInputValue(new Date()), [Validators.required]],
    validUntil: [toDateInputValue(new Date()), [Validators.required]],
    status: ['BORRADOR' as QuotationStatus, [Validators.required]],
    observations: [''],
    discount: ['0'],
    currency: ['USD', [Validators.required]],
    items: this.fb.array([]),
  });

  readonly statusForm = this.fb.group({
    status: ['BORRADOR' as QuotationStatus, [Validators.required]],
  });

  get items(): FormArray {
    return this.form.controls.items as FormArray;
  }

  readonly totals = computed(() => {
    this.formVersion();

    if (this.showStatusOnlyEditor() && this.existingQuotation()) {
      const quotation = this.existingQuotation()!;
      return {
        subtotal: Number(quotation.subtotal),
        discount: Number(quotation.discount),
        vat: Number(quotation.vatValueHistorical),
        total: Number(quotation.total),
      };
    }

    let subtotal = 0;
    let vat = 0;
    let grossTotal = 0;

    this.items.controls.forEach((group) => {
      const value = group.value as Record<string, string>;
      const quantity = this.parseNumber(value['quantity']);
      const vatPercent = this.parseNumber(value['vatPercent']);
      const marginPercent = this.parseNumber(value['marginPercent']);
      const baseCost = this.baseCostFor(value);

      const lineSubtotal = baseCost * quantity;
      const lineVat = baseCost * (vatPercent / 100) * quantity;
      const lineTotal =
        baseCost * (1 + vatPercent / 100) * (1 + marginPercent / 100) * quantity;

      subtotal += lineSubtotal;
      vat += lineVat;
      grossTotal += lineTotal;
    });

    const discount = this.parseNumber(this.form.controls.discount.value ?? '0');

    return {
      subtotal,
      discount,
      vat,
      total: Math.max(grossTotal - discount, 0),
    };
  });

  constructor() {
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.formVersion.update((value) => value + 1));

    this.loadBaseData();
  }

  get isEditMode(): boolean {
    return !!this.quotationId();
  }

  loadBaseData(): void {
    this.loading.set(true);

    const requests: Record<string, any> = {
      clients: this.api.list<Client>('clients'),
      products: this.api.list<Product>('products'),
      services: this.api.list<Service>('services'),
      settings: this.quotationsService.getSettings(),
    };

    if (this.isEditMode) {
      requests['quotation'] = this.quotationsService.findOne(this.quotationId()!);
    }

    forkJoin(requests).subscribe({
      next: (payload) => {
        const clients = payload['clients'] as Client[];
        const products = payload['products'] as Product[];
        const services = payload['services'] as Service[];
        const settings = payload['settings'] as QuotationSetting;

        this.clients.set(clients);
        this.products.set(products);
        this.services.set(services);
        this.settings.set(settings);
        this.form.patchValue({
          currency: settings.defaultCurrency,
          discount: '0',
        });

        const quotation = payload['quotation'] as Quotation | undefined;
        if (quotation) {
          this.existingQuotation.set(quotation);
          this.statusForm.patchValue({ status: quotation.status });
          if (quotation.status === 'BORRADOR') {
            this.patchDraftForm(quotation, settings);
          }
        } else if (!this.items.length) {
          this.addItem();
        }
      },
      complete: () => this.loading.set(false),
    });
  }

  addItem(): void {
    const settings = this.settings();
    const vatOptions = this.vatOptions();
    const marginOptions = this.marginOptions();

    const defaultVat = this.pickAllowedPercent(
      settings?.currentVat,
      vatOptions,
      '15',
    );
    const defaultMargin = this.pickAllowedPercent(
      settings?.defaultMargin,
      marginOptions,
      '20',
    );

    this.items.push(
      this.fb.group({
        itemType: ['PRODUCTO', [Validators.required]],
        productId: [''],
        serviceId: [''],
        description: [''],
        quantity: ['1', [Validators.required]],
        vatPercent: [defaultVat, [Validators.required]],
        marginPercent: [defaultMargin, [Validators.required]],
      }),
    );
    this.formVersion.update((value) => value + 1);
  }

  removeItem(index: number): void {
    this.items.removeAt(index);
    this.formVersion.update((value) => value + 1);
  }

  lineTotal(index: number): string {
    const value = this.items.at(index).value as Record<string, string>;
    const quantity = this.parseNumber(value['quantity']);
    const vatPercent = this.parseNumber(value['vatPercent']);
    const marginPercent = this.parseNumber(value['marginPercent']);
    const base = this.baseCostFor(value);

    const total =
      base * (1 + vatPercent / 100) * (1 + marginPercent / 100) * quantity;
    return toMoney(total, this.form.controls.currency.value ?? 'USD');
  }

  baseCostFor(value: Record<string, string>): number {
    if (value['itemType'] === 'PRODUCTO') {
      const product = this.products().find((item) => item.id === value['productId']);
      return Number(product?.baseCost ?? 0);
    }

    const service = this.services().find((item) => item.id === value['serviceId']);
    return Number(service?.baseCost ?? 0);
  }

  submitQuotation(): void {
    if (this.showStatusOnlyEditor()) {
      return;
    }

    if (this.form.invalid || !this.items.length || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue() as {
      clientId: string;
      issuedAt: string;
      validUntil: string;
      status: QuotationStatus;
      observations: string;
      discount: string;
      currency: string;
      items: Record<string, string>[];
    };

    const items: QuotationItemPayload[] = raw.items.map((item) => ({
      itemType: item['itemType'] as 'PRODUCTO' | 'SERVICIO',
      productId: item['itemType'] === 'PRODUCTO' ? item['productId'] : undefined,
      serviceId: item['itemType'] === 'SERVICIO' ? item['serviceId'] : undefined,
      description: item['description'] || undefined,
      quantity: this.normalizePositiveIntegerString(item['quantity'], '1'),
      vatPercent: this.normalizeIntegerString(
        item['vatPercent'],
        this.pickAllowedPercent(this.settings()?.currentVat, this.vatOptions(), '15'),
      ),
      marginPercent: this.normalizeIntegerString(
        item['marginPercent'],
        this.pickAllowedPercent(
          this.settings()?.defaultMargin,
          this.marginOptions(),
          '20',
        ),
      ),
    }));

    const payload = {
      clientId: raw.clientId,
      issuedAt: raw.issuedAt,
      validUntil: raw.validUntil,
      status: raw.status,
      observations: raw.observations || undefined,
      discount: this.normalizeDecimalString(raw.discount || '0', '0'),
      currency: raw.currency,
      items,
    };

    this.saving.set(true);

    const request$ = this.isEditMode
      ? this.quotationsService.updateDraft(this.quotationId()!, payload)
      : this.quotationsService.create(payload);

    request$.subscribe({
      next: (quotation) => {
        this.notifications.success(
          this.isEditMode
            ? 'Cotización borrador actualizada correctamente.'
            : 'Cotización creada correctamente.',
        );
        void this.router.navigate(['/quotations', quotation.id]);
      },
      complete: () => this.saving.set(false),
    });
  }

  submitStatusUpdate(): void {
    if (!this.quotationId() || this.statusForm.invalid || this.saving()) {
      return;
    }

    const status = this.statusForm.controls.status.value as QuotationStatus;

    this.saving.set(true);

    this.quotationsService.updateStatus(this.quotationId()!, status).subscribe({
      next: () => {
        this.notifications.success('Estado actualizado correctamente.');
        void this.router.navigate(['/quotations', this.quotationId()]);
      },
      complete: () => this.saving.set(false),
    });
  }

  money(value: number): string {
    return toMoney(value, this.form.controls.currency.value ?? 'USD');
  }

  private patchDraftForm(
    quotation: Quotation,
    settings: QuotationSetting,
  ): void {
    this.form.patchValue({
      clientId: quotation.client.id,
      issuedAt: quotation.issuedAt,
      validUntil: quotation.validUntil,
      status: quotation.status,
      observations: quotation.observations ?? '',
      discount: this.normalizeDecimalString(quotation.discount, '0'),
      currency: quotation.currency || settings.defaultCurrency,
    });

    this.items.clear();
    quotation.details.forEach((detail) => {
      this.items.push(
        this.fb.group({
          itemType: [detail.itemType, [Validators.required]],
          productId:
            detail.itemType === 'PRODUCTO' ? detail.referenceId : '',
          serviceId:
            detail.itemType === 'SERVICIO' ? detail.referenceId : '',
          description: [detail.descriptionFrozen ?? ''],
          quantity: [
            this.normalizePositiveIntegerString(detail.quantity, '1'),
            [Validators.required],
          ],
          vatPercent: [
            this.pickAllowedPercent(
              detail.vatPercentHistorical,
              this.vatOptions(),
              this.pickAllowedPercent(settings.currentVat, this.vatOptions(), '15'),
            ),
            [Validators.required],
          ],
          marginPercent: [
            this.pickAllowedPercent(
              detail.marginPercentHistorical,
              this.marginOptions(),
              this.pickAllowedPercent(
                settings.defaultMargin,
                this.marginOptions(),
                '20',
              ),
            ),
            [Validators.required],
          ],
        }),
      );
    });

    if (!this.items.length) {
      this.addItem();
    }

    this.formVersion.update((value) => value + 1);
  }

  private normalizeAllowedPercentOptions(
    rawValues: string[] | undefined,
    fallback: string[],
  ): string[] {
    const source = rawValues?.length ? rawValues : fallback;
    const unique = new Set<string>();

    source
      .flatMap((value) => String(value).split(','))
      .map((value) => this.normalizeIntegerString(value, '0'))
      .forEach((value) => unique.add(value));

    return Array.from(unique.values());
  }

  private pickAllowedPercent(
    value: string | undefined,
    options: string[],
    fallback: string,
  ): string {
    const normalized = this.normalizeIntegerString(value ?? fallback, fallback);
    if (options.includes(normalized)) {
      return normalized;
    }
    return options[0] ?? fallback;
  }

  private parseNumber(value: unknown): number {
    const normalized = String(value ?? '0').replace(',', '.').trim();
    const parsed = Number(normalized);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  private normalizeIntegerString(value: unknown, fallback: string): string {
    const parsed = this.parseNumber(value);
    if (!Number.isFinite(parsed) || parsed < 0) {
      return fallback;
    }
    return String(Math.round(parsed));
  }

  private normalizePositiveIntegerString(value: unknown, fallback: string): string {
    const parsed = this.parseNumber(value);
    if (!Number.isFinite(parsed) || parsed <= 0) {
      return fallback;
    }
    return String(Math.round(parsed));
  }

  private normalizeDecimalString(value: unknown, fallback: string): string {
    const parsed = this.parseNumber(value);
    if (!Number.isFinite(parsed)) {
      return fallback;
    }
    return String(parsed);
  }
}
