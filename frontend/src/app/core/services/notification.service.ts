import { Injectable, signal } from '@angular/core';

export interface ToastMessage {
  id: number;
  type: 'success' | 'error' | 'info';
  message: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly toasts = signal<ToastMessage[]>([]);
  private counter = 0;

  success(message: string): void {
    this.push('success', message);
  }

  error(message: string): void {
    this.push('error', message);
  }

  info(message: string): void {
    this.push('info', message);
  }

  remove(id: number): void {
    this.toasts.update((all) => all.filter((toast) => toast.id !== id));
  }

  private push(type: ToastMessage['type'], message: string): void {
    const id = ++this.counter;
    this.toasts.update((all) => [...all, { id, type, message }]);

    setTimeout(() => this.remove(id), 3500);
  }
}
