import { Component, signal } from '@angular/core';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { SEARCH_DEBOUNCE_MS, onSearchChange } from './search.util';

@Component({ selector: 'app-search-host', template: '' })
class SearchHostComponent {
  readonly query = signal('');
  calls = 0;

  constructor() {
    onSearchChange(this.query, () => this.calls++);
  }
}

describe('onSearchChange', () => {
  const setup = () => {
    const fixture = TestBed.createComponent(SearchHostComponent);
    fixture.detectChanges();
    return { fixture, host: fixture.componentInstance };
  };

  const type = (fixture: ReturnType<typeof setup>['fixture'], value: string) => {
    fixture.componentInstance.query.set(value);
    fixture.detectChanges();
  };

  it('no dispara con el valor inicial', fakeAsync(() => {
    const { host } = setup();
    tick(SEARCH_DEBOUNCE_MS * 2);
    expect(host.calls).toBe(0);
  }));

  it('dispara una sola vez cuando el usuario deja de escribir', fakeAsync(() => {
    const { fixture, host } = setup();
    type(fixture, 'c');
    tick(100);
    type(fixture, 'ca');
    tick(100);
    type(fixture, 'cam');
    tick(SEARCH_DEBOUNCE_MS);
    expect(host.calls).toBe(1);
  }));

  it('ignora cambios que solo agregan espacios', fakeAsync(() => {
    const { fixture, host } = setup();
    type(fixture, 'domo');
    tick(SEARCH_DEBOUNCE_MS);
    type(fixture, 'domo  ');
    tick(SEARCH_DEBOUNCE_MS);
    expect(host.calls).toBe(1);
  }));

  it('se da de baja al destruir el componente', fakeAsync(() => {
    const { fixture, host } = setup();
    type(fixture, 'a');
    fixture.destroy();
    tick(SEARCH_DEBOUNCE_MS);
    expect(host.calls).toBe(0);
  }));
});
