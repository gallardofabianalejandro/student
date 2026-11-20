import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

/**
 * Lleva a una pagina de error cuando la API responde algo que el usuario no puede resolver en el lugar:
 * 403 (sin permiso, en cualquier metodo) y 5xx al cargar datos (GET). Un 5xx al guardar o borrar no navega,
 * para que el formulario con lo que escribio no se pierda: ahi la pantalla muestra su propio aviso.
 * Los endpoints de autenticacion (/api/auth/) muestran sus errores en el formulario. El error siempre se re-lanza.
 */
export const errorPageInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  if (!req.url.startsWith('/api/') || req.url.startsWith('/api/auth/')) {
    return next(req);
  }

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        // replaceUrl: la pantalla que fallo no queda en el historial, asi "Atras" no devuelve al usuario
        // a la misma carga que vuelve a fallar. "from" permite que /error ofrezca reintentar esa pantalla.
        if (error.status === 403) {
          router.navigateByUrl('/forbidden', { replaceUrl: true });
        } else if (error.status >= 500 && req.method === 'GET') {
          router.navigateByUrl('/error', { replaceUrl: true, state: { from: router.url } });
        }
      }
      return throwError(() => error);
    }),
  );
};
