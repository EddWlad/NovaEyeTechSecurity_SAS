import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SearchSelectComponent, SearchSelectOption } from './search-select.component';

const OPTIONS: SearchSelectOption[] = [
  { value: 'p1', label: 'CÁMARA DOMO 1080P', detail: 'CD-01 · HIKVISION' },
  { value: 'p2', label: 'Cámara bala 4MP', detail: 'CB-02 · EZVIZ' },
  { value: 'p3', label: 'Cable UTP cat6', detail: 'UTP-6 · Genérico' },
];

describe('SearchSelectComponent', () => {
  let fixture: ComponentFixture<SearchSelectComponent>;
  let component: SearchSelectComponent;
  let input: HTMLInputElement;
  let changes: string[];

  const key = (name: string) => {
    input.dispatchEvent(new KeyboardEvent('keydown', { key: name }));
    fixture.detectChanges();
  };

  const typeText = (text: string) => {
    input.value = text;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };

  const labels = () => component.filtered().map((option) => option.value);

  beforeEach(() => {
    fixture = TestBed.createComponent(SearchSelectComponent);
    fixture.componentRef.setInput('options', OPTIONS);
    component = fixture.componentInstance;
    changes = [];
    component.registerOnChange((value) => changes.push(value));
    fixture.detectChanges();
    input = fixture.nativeElement.querySelector('input');
  });

  it('filtra sin distinguir tildes ni mayusculas', () => {
    input.dispatchEvent(new Event('focus'));
    typeText('camara');
    expect(labels()).toEqual(['p1', 'p2']);
  });

  it('busca por palabras en cualquier orden y tambien en el detalle', () => {
    input.dispatchEvent(new Event('focus'));
    typeText('domo camara');
    expect(labels()).toEqual(['p1']);

    typeText('ezviz');
    expect(labels()).toEqual(['p2']);
  });

  it('elige con el teclado y avisa al formulario', () => {
    input.dispatchEvent(new Event('focus'));
    fixture.detectChanges();
    key('ArrowDown');
    key('Enter');

    expect(changes).toEqual(['p2']);
    expect(component.open()).toBeFalse();
    expect(input.value).toBe('Cámara bala 4MP');
  });

  it('Enter sin resultados no envia el formulario', () => {
    input.dispatchEvent(new Event('focus'));
    typeText('zzz');
    const enter = new KeyboardEvent('keydown', { key: 'Enter', cancelable: true });
    input.dispatchEvent(enter);

    expect(enter.defaultPrevented).toBeTrue();
    expect(changes).toEqual([]);
  });

  it('Escape cierra sin cambiar el valor', () => {
    component.writeValue('p3');
    input.dispatchEvent(new Event('focus'));
    typeText('domo');
    key('Escape');

    expect(changes).toEqual([]);
    expect(input.value).toBe('Cable UTP cat6');
  });

  it('muestra la etiqueta del valor que viene del formulario (modo edicion)', () => {
    component.writeValue('p1');
    fixture.detectChanges();
    expect(input.value).toBe('CÁMARA DOMO 1080P');
  });

  it('limita las opciones visibles y avisa que hay mas', () => {
    const many = Array.from({ length: 60 }, (_, i) => ({ value: `v${i}`, label: `Producto ${i}` }));
    fixture.componentRef.setInput('options', many);
    input.dispatchEvent(new Event('focus'));
    fixture.detectChanges();

    expect(component.filtered().length).toBe(50);
    expect(component.hasMore()).toBeTrue();
  });

  it('respeta el estado deshabilitado del formulario', () => {
    component.setDisabledState(true);
    fixture.detectChanges();
    expect(input.disabled).toBeTrue();
  });
});
