import { NgClass, NgFor } from '@angular/common';
import { Component, inject } from '@angular/core';

import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-toast-stack',
  standalone: true,
  imports: [NgClass, NgFor],
  template: `
    <div class="toast-stack">
      <article *ngFor="let toast of notificationService.toasts()" [ngClass]="toast.type" class="toast-item">
        <span class="material-symbols-outlined">{{ icon(toast.type) }}</span>
        <span>{{ toast.message }}</span>
      </article>
    </div>
  `,
  styles: `
    .toast-stack {
      position: fixed;
      top: 1rem;
      right: 1rem;
      z-index: 1200;
      display: flex;
      flex-direction: column;
      gap: .55rem;
    }

    .toast-item {
      min-width: 250px;
      max-width: 360px;
      color: white;
      border-radius: 10px;
      padding: .65rem .75rem;
      display: flex;
      align-items: center;
      gap: .45rem;
      box-shadow: var(--shadow-md);
      font-size: .85rem;
    }

    .success { background: #0f766e; }
    .error { background: #b91c1c; }
    .info { background: #1d4ed8; }
  `,
})
export class ToastStackComponent {
  readonly notificationService = inject(NotificationService);

  icon(type: string): string {
    if (type === 'success') {
      return 'check_circle';
    }
    if (type === 'error') {
      return 'error';
    }
    return 'info';
  }
}
