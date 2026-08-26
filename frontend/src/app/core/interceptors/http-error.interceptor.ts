import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const notifications = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const message =
        (error.error as { message?: string })?.message ??
        error.message ??
        'Ocurrió un error de comunicación.';

      if (error.status === 401 && !req.url.includes('/auth/login')) {
        auth.logout();
        notifications.error('Sesión expirada. Inicia sesión nuevamente.');
      } else if (error.status >= 400) {
        notifications.error(message);
      }

      return throwError(() => error);
    }),
  );
};
