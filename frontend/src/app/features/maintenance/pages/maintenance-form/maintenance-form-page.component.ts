import { NgFor, NgIf } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';

import { Client, Maintenance, User } from '../../../../core/models/entities.models';
import { AuthService } from '../../../../core/services/auth.service';
import { ApiService } from '../../../../core/services/api.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { toDateInputValue } from '../../../../core/utils/format.util';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header.component';
import { MaintenanceService } from '../../services/maintenance.service';

@Component({
  selector: 'app-maintenance-form-page',
  standalone: true,
  imports: [NgFor, NgIf, RouterLink, ReactiveFormsModule, PageHeaderComponent, LoadingSpinnerComponent],
  templateUrl: './maintenance-form-page.component.html',
})
export class MaintenanceFormPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(ApiService);
  private readonly authService = inject(AuthService);
  private readonly maintenanceService = inject(MaintenanceService);
  private readonly notifications = inject(NotificationService);

  readonly maintenanceId = signal<string | null>(this.route.snapshot.paramMap.get('id'));
  readonly loading = signal(true);
  readonly saving = signal(false);

  readonly clients = signal<Client[]>([]);
  readonly technicians = signal<User[]>([]);

  readonly form = this.fb.group({
    clientId: ['', [Validators.required]],
    type: ['PREVENTIVO', [Validators.required]],
    status: ['PENDIENTE', [Validators.required]],
    scheduledDate: [toDateInputValue(new Date()), [Validators.required]],
    executionDate: [''],
    technicianId: [''],
    intervenedSystem: ['', [Validators.required]],
    diagnosis: ['', [Validators.required]],
    appliedSolution: ['', [Validators.required]],
    observations: [''],
  });

  constructor() {
    this.load();
  }

  get isEditMode(): boolean {
    return !!this.maintenanceId();
  }

  load(): void {
    this.loading.set(true);

    this.api.list<Client>('clients').subscribe({ next: (rows) => this.clients.set(rows) });

    this.maintenanceService
      .listUsers()
      .pipe(catchError(() => of([])))
      .subscribe({ next: (rows) => this.technicians.set(rows.filter((item: any) => item.role === 'TECNICO')) });

    if (!this.isEditMode) {
      const currentUser = this.authService.user();
      if (currentUser?.role === 'TECNICO') {
        this.form.patchValue({ technicianId: currentUser.id });
      }
      this.loading.set(false);
      return;
    }

    this.maintenanceService.findOne(this.maintenanceId()!).subscribe({
      next: (maintenance) => {
        this.form.patchValue({
          clientId: maintenance.client.id,
          type: maintenance.type,
          status: maintenance.status,
          scheduledDate: maintenance.scheduledDate,
          executionDate: maintenance.executionDate ?? '',
          technicianId: maintenance.technician.id,
          intervenedSystem: maintenance.intervenedSystem,
          diagnosis: maintenance.diagnosis,
          appliedSolution: maintenance.appliedSolution,
          observations: maintenance.observations ?? '',
        });
      },
      complete: () => this.loading.set(false),
    });
  }

  submit(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const payload = {
      clientId: raw.clientId ?? '',
      type: (raw.type ?? 'PREVENTIVO') as 'PREVENTIVO' | 'CORRECTIVO',
      status: (raw.status ?? 'PENDIENTE') as 'PENDIENTE' | 'EN_PROCESO' | 'COMPLETADO' | 'CANCELADO',
      scheduledDate: raw.scheduledDate ?? '',
      technicianId: raw.technicianId || undefined,
      executionDate: raw.executionDate || undefined,
      intervenedSystem: raw.intervenedSystem ?? '',
      diagnosis: raw.diagnosis ?? '',
      appliedSolution: raw.appliedSolution ?? '',
      observations: raw.observations || undefined,
    };

    this.saving.set(true);

    const request$ = this.isEditMode
      ? this.maintenanceService.update(this.maintenanceId()!, payload)
      : this.maintenanceService.create(payload as never);

    request$.subscribe({
      next: (maintenance: Maintenance) => {
        this.notifications.success(`Mantenimiento ${this.isEditMode ? 'actualizado' : 'creado'} correctamente.`);
        void this.router.navigate(['/maintenance', maintenance.id]);
      },
      complete: () => this.saving.set(false),
    });
  }
}
