import { Signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { distinctUntilChanged, map, skip, switchMap, timer } from 'rxjs';

/** Espera tras la ultima tecla antes de buscar: una peticion por pausa, no una por letra. */
export const SEARCH_DEBOUNCE_MS = 350;

/**
 * Ejecuta `onChange` cuando el texto de busqueda cambia y el usuario deja de escribir.
 *
 * Ignora el valor inicial (la pantalla ya hace su primera carga) y los cambios que solo agregan
 * espacios. Debe llamarse en un contexto de inyeccion (el constructor del componente): se da de baja
 * sola al destruirse.
 *
 * La espera va con `switchMap` + `timer` y no con `debounceTime`: al destruirse el componente la
 * senal completa, y `debounceTime` emitiria el texto pendiente, lanzando una busqueda en una pantalla
 * que ya no existe.
 */
export const onSearchChange = (search: Signal<string>, onChange: () => void): void => {
  toObservable(search)
    .pipe(
      skip(1),
      map((value) => value.trim()),
      switchMap((value) => timer(SEARCH_DEBOUNCE_MS).pipe(map(() => value))),
      distinctUntilChanged(),
      takeUntilDestroyed(),
    )
    .subscribe(() => onChange());
};
