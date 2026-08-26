import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  template: `
    <div class="card" style="padding: 1.5rem; text-align: center; color: var(--gray-600)">
      <span class="material-symbols-outlined" style="font-size: 2rem; color: var(--gray-300)">{{ icon }}</span>
      <p style="margin: .6rem 0 0; font-weight: 600; color: var(--gray-800)">{{ title }}</p>
      <p style="margin: .25rem 0 0; font-size: .85rem">{{ description }}</p>
    </div>
  `,
})
export class EmptyStateComponent {
  @Input() icon = 'inbox';
  @Input() title = 'Sin resultados';
  @Input() description = 'No hay datos disponibles para mostrar.';
}
