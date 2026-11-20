import { registerLocaleData } from '@angular/common';
import localeEsAr from '@angular/common/locales/es-AR';
import { ApplicationConfig, LOCALE_ID, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { TitleStrategy, provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { authInterceptor } from './interceptors/auth.interceptor';
import { errorPageInterceptor } from './interceptors/error-page.interceptor';
import { refreshTokenInterceptor } from './interceptors/refresh-token.interceptor';
import { PageTitleStrategy } from './page-title.strategy';
import { AuthService } from './services/auth.service';

registerLocaleData(localeEsAr);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    { provide: TitleStrategy, useClass: PageTitleStrategy },
    { provide: LOCALE_ID, useValue: 'es-AR' },
    // errorPageInterceptor va primero: ve el error final, despues de que refreshTokenInterceptor renovo el token.
    provideHttpClient(withInterceptors([errorPageInterceptor, refreshTokenInterceptor, authInterceptor])),
    provideAppInitializer(() => inject(AuthService).restoreSession()),
  ],
};
