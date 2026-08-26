import { NgIf } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-pagination-controls',
  standalone: true,
  imports: [NgIf],
  template: `
    <div class="pagination" *ngIf="totalPages > 0">
      <p class="pagination-summary">
        Página <strong>{{ page }}</strong> de <strong>{{ totalPages }}</strong>
        <span>• {{ total }} registros</span>
      </p>

      <div class="pagination-actions">
        <button
          class="btn btn-secondary btn-sm"
          type="button"
          [disabled]="page <= 1"
          (click)="goTo(page - 1)"
        >
          Anterior
        </button>
        <button
          class="btn btn-secondary btn-sm"
          type="button"
          [disabled]="page >= totalPages"
          (click)="goTo(page + 1)"
        >
          Siguiente
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .pagination {
        margin-top: 0.7rem;
        display: flex;
        gap: 0.65rem;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
      }

      .pagination-summary {
        margin: 0;
        color: var(--gray-600);
        font-size: 0.85rem;
      }

      .pagination-actions {
        display: flex;
        gap: 0.45rem;
      }
    `,
  ],
})
export class PaginationControlsComponent {
  @Input() page = 1;
  @Input() totalPages = 1;
  @Input() total = 0;

  @Output() pageChange = new EventEmitter<number>();

  goTo(nextPage: number): void {
    if (nextPage < 1 || nextPage > this.totalPages || nextPage === this.page) {
      return;
    }
    this.pageChange.emit(nextPage);
  }
}
