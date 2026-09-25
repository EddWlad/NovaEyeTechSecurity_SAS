import { HttpEvent, HttpInterceptorFn } from '@angular/common/http';
import { Observable, finalize, share } from 'rxjs';

const inFlight = new Map<string, Observable<HttpEvent<unknown>>>();

/**
 * Unifica los GET identicos que estan en vuelo al mismo tiempo en una sola peticion de red.
 *
 * Al abrir el detalle o la edicion de un registro, la pantalla y la miga de pan piden el mismo
 * recurso en el mismo instante; sin esto viajaban dos peticiones identicas. No guarda nada: en cuanto
 * la respuesta llega, el siguiente GET va a la red de nuevo, asi que nunca sirve datos viejos.
 *
 * Las respuestas se comparten por referencia entre quienes pidieron lo mismo: quien reciba el cuerpo
 * no debe mutarlo (ordenarlo en sitio, por ejemplo); si necesita cambiarlo, que lo copie.
 *
 * Va primero en la cadena: el interceptor de errores queda por debajo, y con una sola peticion
 * compartida un fallo muestra un solo aviso en vez de dos.
 */
export const dedupeGetInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.method !== 'GET') {
    return next(req);
  }

  const key = `${req.responseType}|${req.urlWithParams}`;
  const pending = inFlight.get(key);
  if (pending) {
    return pending;
  }

  const shared = next(req).pipe(
    finalize(() => inFlight.delete(key)),
    share(),
  );
  inFlight.set(key, shared);

  return shared;
};
