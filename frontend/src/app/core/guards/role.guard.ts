import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { AuthService } from '../services/auth.service';
import { Role } from '../models/enums';

export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const roles = (route.data?.['roles'] ?? []) as Role[];

  if (!roles.length || auth.hasRole(roles)) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};
