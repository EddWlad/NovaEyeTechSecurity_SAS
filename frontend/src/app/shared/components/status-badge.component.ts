import { NgClass } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [NgClass],
  template: `<span class="badge" [ngClass]="className">{{ label }}</span>`,
})
export class StatusBadgeComponent {
  @Input() status = '';

  get label(): string {
    return this.status.replaceAll('_', ' ');
  }

  get className(): string {
    const normalized = this.status.toUpperCase();

    if (['ACTIVO', 'APROBADA', 'COMPLETADO', 'ENVIADA'].includes(normalized)) {
      return 'badge-success';
    }
    if (['EN_PROCESO', 'PENDIENTE', 'BORRADOR'].includes(normalized)) {
      return 'badge-warning';
    }
    if (['INACTIVO', 'RECHAZADA', 'CANCELADO'].includes(normalized)) {
      return 'badge-danger';
    }
    return 'badge-neutral';
  }
}
