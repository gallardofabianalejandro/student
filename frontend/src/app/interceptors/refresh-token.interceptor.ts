import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

const AUTH_ENDPOINTS = ['/api/auth/login', '/api/auth/refresh', '/api/auth/register', '/api/auth/logout'];

/** Ante un 401 en la API renueva el access token una vez y reintenta; si falla, cierra la sesion local. */
export const refreshTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!req.url.startsWith('/api/') || AUTH_ENDPOINTS.some((path) => req.url.startsWith(path))) {
    return next(req);
  }

  return next(req).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }
      return auth.refreshAccessToken().pipe(
        switchMap(() => next(req)),
        catchError((refreshError: unknown) => {
          auth.clearSession();
          router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
