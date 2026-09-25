import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { PreloadAllModules, provideRouter, withNavigationErrorHandler, withPreloading } from '@angular/router';

import { routes } from './app.routes';
import { authTokenInterceptor } from './core/interceptors/auth-token.interceptor';
import { dedupeGetInterceptor } from './core/interceptors/dedupe-get.interceptor';
import { httpErrorInterceptor } from './core/interceptors/http-error.interceptor';

const RELOAD_FLAG = 'chunk-reload-url';

/**
 * Tras un despliegue, los archivos con hash de la version anterior ya no existen: un usuario que
 * tenia la app abierta y navega a una pantalla aun no descargada recibe un error de carga. Se
 * recarga la pagina una sola vez para tomar la version nueva (la marca evita un bucle si el problema
 * es otro).
 */
const reloadOnStaleChunk = ({ error, url }: { error: unknown; url: string }): void => {
  const message = String((error as { message?: string } | null)?.message ?? error);
  const isChunkError = /dynamically imported module|Loading chunk|Importing a module script failed/i.test(message);

  if (!isChunkError || sessionStorage.getItem(RELOAD_FLAG) === url) {
    return;
  }

  sessionStorage.setItem(RELOAD_FLAG, url);
  window.location.assign(url);
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    // Con rutas lazy, el resto de pantallas se descarga en segundo plano tras el arranque: la
    // primera visita a cada una es instantanea y el bundle inicial sigue siendo pequeno.
    provideRouter(routes, withPreloading(PreloadAllModules), withNavigationErrorHandler(reloadOnStaleChunk)),
    provideHttpClient(withInterceptors([dedupeGetInterceptor, authTokenInterceptor, httpErrorInterceptor])),
  ]
};
