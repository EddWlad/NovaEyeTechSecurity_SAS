import { NgIf } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [NgIf],
  template: `
    <div>
      <h1 class="page-title">{{ title }}</h1>
      <p class="page-subtitle" *ngIf="subtitle">{{ subtitle }}</p>
    </div>
  `,
})
export class PageHeaderComponent {
  @Input({ required: true }) title = '';
  @Input() subtitle = '';
}
