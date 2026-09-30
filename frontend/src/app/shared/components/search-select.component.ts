import { NgFor, NgIf } from '@angular/common';
import { Component, computed, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface SearchSelectOption {
  value: string;
  label: string;
  /** Texto secundario que tambien se busca (codigo, marca, categoria). */
  detail?: string;
}

/** Tope de opciones visibles: con cientos de productos la lista completa no se puede recorrer. */
const MAX_RESULTS = 50;

const normalize = (text: string): string =>
  text
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase();

let nextId = 0;

/**
 * Selector con busqueda para listas largas (684 productos). Se escribe para filtrar por nombre o
 * detalle, por palabras en cualquier orden y sin distinguir tildes ni mayusculas, y se elige con clic o con el teclado (flechas, Enter,
 * Escape). Se usa como un control de formulario mas: `formControlName="productId"`.
 *
 * La lista se muestra dentro del flujo y no flotante: vive en celdas de tablas con scroll, que la
 * recortarian.
 */
@Component({
  selector: 'app-search-select',
  standalone: true,
  imports: [NgFor, NgIf],
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SearchSelectComponent), multi: true }],
  template: `
    <div class="search-select">
      <input
        class="input"
        type="text"
        role="combobox"
        autocomplete="off"
        [id]="inputId"
        [attr.aria-expanded]="open()"
        [attr.aria-controls]="listId"
        [attr.aria-activedescendant]="open() && filtered().length ? optionId(activeIndex()) : null"
        [attr.aria-label]="placeholder()"
        [placeholder]="placeholder()"
        [disabled]="disabled()"
        [value]="open() ? term() : selectedLabel()"
        (focus)="openList()"
        (input)="onType($any($event.target).value)"
        (keydown)="onKeydown($event)"
        (blur)="close()"
      />

      <ul *ngIf="open()" class="search-select-list" role="listbox" [id]="listId">
        <!-- El teclado se maneja en el input (aria-activedescendant): las opciones no reciben foco. -->
        <!-- eslint-disable-next-line @angular-eslint/template/click-events-have-key-events, @angular-eslint/template/interactive-supports-focus -->
        <li
          *ngFor="let option of filtered(); let i = index"
          role="option"
          [id]="optionId(i)"
          [attr.aria-selected]="option.value === value()"
          [class.active]="i === activeIndex()"
          (mousedown)="$event.preventDefault()"
          (click)="choose(option)"
        >
          <span class="label">{{ option.label }}</span>
          <small *ngIf="option.detail">{{ option.detail }}</small>
        </li>
        <li *ngIf="!filtered().length" class="empty" role="presentation">Sin resultados</li>
        <li *ngIf="hasMore()" class="empty" role="presentation">Escribe para acotar la búsqueda</li>
      </ul>
    </div>
  `,
  styles: `
    .search-select {
      display: grid;
      gap: 0.3rem;
      min-width: 0;
    }

    .search-select-list {
      list-style: none;
      margin: 0;
      padding: 0.25rem;
      max-height: 240px;
      overflow-y: auto;
      border: 1px solid var(--gray-300);
      border-radius: 10px;
      background: var(--white);
      box-shadow: var(--shadow-sm);
    }

    li[role='option'] {
      display: grid;
      gap: 0.1rem;
      padding: 0.45rem 0.55rem;
      border-radius: 8px;
      cursor: pointer;
    }

    li[role='option'].active,
    li[role='option']:hover {
      background: rgb(var(--brand-rgb) / 8%);
    }

    li[role='option'][aria-selected='true'] .label {
      font-weight: 700;
      color: var(--brand-600);
    }

    small {
      color: var(--gray-600);
      font-size: 0.72rem;
    }

    .empty {
      padding: 0.45rem 0.55rem;
      color: var(--gray-600);
      font-size: 0.8rem;
    }
  `,
})
export class SearchSelectComponent implements ControlValueAccessor {
  readonly options = input<SearchSelectOption[]>([]);
  readonly placeholder = input('Buscar...');

  readonly value = signal('');
  readonly term = signal('');
  readonly open = signal(false);
  readonly activeIndex = signal(0);
  readonly disabled = signal(false);

  readonly inputId = `search-select-${nextId++}`;
  readonly listId = `${this.inputId}-list`;

  // Cada palabra debe aparecer, en cualquier orden: "domo camara" encuentra "CÁMARA DOMO".
  private readonly matches = computed(() => {
    const words = normalize(this.term()).split(/\s+/).filter(Boolean);
    const options = this.options();
    if (!words.length) {
      return options;
    }
    return options.filter((option) => {
      const text = normalize(`${option.label} ${option.detail ?? ''}`);
      return words.every((word) => text.includes(word));
    });
  });

  readonly filtered = computed(() => this.matches().slice(0, MAX_RESULTS));
  readonly hasMore = computed(() => this.matches().length > MAX_RESULTS);

  readonly selectedLabel = computed(
    () => this.options().find((option) => option.value === this.value())?.label ?? '',
  );

  private onChange: (value: string) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  optionId(index: number): string {
    return `${this.inputId}-option-${index}`;
  }

  openList(): void {
    this.term.set('');
    this.activeIndex.set(0);
    this.open.set(true);
  }

  onType(text: string): void {
    this.term.set(text);
    this.activeIndex.set(0);
    this.open.set(true);
  }

  choose(option: SearchSelectOption): void {
    this.value.set(option.value);
    this.onChange(option.value);
    this.open.set(false);
  }

  close(): void {
    this.open.set(false);
    this.onTouched();
  }

  onKeydown(event: KeyboardEvent): void {
    const last = this.filtered().length - 1;

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        if (!this.open()) {
          this.openList();
          return;
        }
        this.activeIndex.set(Math.min(this.activeIndex() + 1, last));
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.activeIndex.set(Math.max(this.activeIndex() - 1, 0));
        break;
      case 'Enter': {
        if (!this.open()) {
          break;
        }
        // Con la lista abierta, Enter nunca envia el formulario, aunque no haya resultados.
        event.preventDefault();
        const option = this.filtered()[this.activeIndex()];
        if (option) {
          this.choose(option);
        }
        break;
      }
      case 'Escape':
        this.open.set(false);
        break;
    }
  }

  writeValue(value: string | null): void {
    this.value.set(value ?? '');
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.disabled.set(disabled);
  }
}
