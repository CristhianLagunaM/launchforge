import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from '../auth/auth.store';

export const roleGuard: CanActivateFn = (route) => {
  const authStore = inject(AuthStore);
  const router = inject(Router);
  const requiredRoles = (route.data['roles'] as string[] | undefined) ?? [];

  if (!authStore.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  const currentRoles = authStore.roles().map((role) => role.replace(/^ROLE_/, '').toUpperCase());
  if (requiredRoles.some((role) => currentRoles.includes(role.replace(/^ROLE_/, '').toUpperCase()))) {
    return true;
  }

  return router.createUrlTree(['/app/forbidden']);
};
