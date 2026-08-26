import { NgFor, NgIf } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-breadcrumbs',
  standalone: true,
  imports: [NgFor, NgIf],
  template: `
    <nav class="crumbs" aria-label="Breadcrumb">
      <ng-container *ngFor="let item of items; let last = last">
        <span [class.current]="last">{{ item }}</span>
        <span *ngIf="!last" class="sep">/</span>
      </ng-container>
    </nav>
  `,
  styles: `
    .crumbs {
      display: inline-flex;
      align-items: center;
      gap: 0.42rem;
      color: var(--gray-600);
      font-size: .8rem;
      max-width: 100%;
      overflow-x: auto;
      white-space: nowrap;
      min-width: 0;
      scrollbar-width: none;
    }

    .crumbs::-webkit-scrollbar {
      display: none;
    }

    .sep {
      color: #9ca3af;
    }

    .current {
      color: var(--gray-900);
      font-weight: 600;
    }

    @media (max-width: 640px) {
      .crumbs {
        font-size: .72rem;
        gap: .3rem;
      }
    }
  `,
})
export class BreadcrumbsComponent {
  @Input() items: string[] = [];
}
