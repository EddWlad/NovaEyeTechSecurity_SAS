import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  template: `
    <div style="display:flex; align-items:center; gap:.55rem; color: var(--gray-600); padding: .6rem 0;">
      <span class="material-symbols-outlined spin">progress_activity</span>
      <span>{{ text }}</span>
    </div>
  `,
  styles: `
    .spin {
      animation: spin 1.1s linear infinite;
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }
  `,
})
export class LoadingSpinnerComponent {
  @Input() text = 'Cargando información...';
}
