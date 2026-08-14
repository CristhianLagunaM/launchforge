import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthStore } from '../auth/auth.store';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authStore = inject(AuthStore);
  const router = inject(Router);
  const token = authStore.accessToken();
  const authRequest = token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;

  return next(authRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !request.url.endsWith('/login')) {
        authStore.handleUnauthorized();
      } else if (error.status === 403) {
        void router.navigate(['/app/forbidden']);
      }
      return throwError(() => error);
    })
  );
};
