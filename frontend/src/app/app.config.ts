import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { PreloadAllModules, provideRouter, withNavigationErrorHandler, withPreloading } from '@angular/router';

import { routes } from './app.routes';
import { authTokenInterceptor } from './core/interceptors/auth-token.interceptor';
import { dedupeGetInterceptor } from './core/interceptors/dedupe-get.interceptor';
import { httpErrorInterceptor } from './core/interceptors/http-error.interceptor';

const RELOAD_AT_KEY = 'chunk-reload-at';
/** Una recarga por chunk viejo como maximo cada 30 s: corta un bucle sin bloquear el siguiente despliegue. */
const RELOAD_COOLDOWN_MS = 30_000;

/**
 * Tras un despliegue, los archivos con hash de la version anterior ya no existen: un usuario que
 * tenia la app abierta y navega a una pantalla aun no descargada recibe un error de carga. Se
 * recarga la pagina para tomar la version nueva; si el error persiste tras recargar (el problema es
 * otro) no se recarga otra vez de inmediato.
 */
const reloadOnStaleChunk = ({ error, url }: { error: unknown; url: string }): void => {
  const message = String((error as { message?: string } | null)?.message ?? error);
  const isChunkError = /dynamically imported module|Loading chunk|Importing a module script failed/i.test(message);

  if (!isChunkError) {
    return;
  }

  const lastReload = Number(sessionStorage.getItem(RELOAD_AT_KEY) ?? 0);
  if (Date.now() - lastReload < RELOAD_COOLDOWN_MS) {
    return;
  }

  sessionStorage.setItem(RELOAD_AT_KEY, String(Date.now()));
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
