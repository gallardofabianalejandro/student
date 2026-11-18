import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let http: HttpTestingController;
  let auth: AuthService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
  });

  const run = () =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/dashboard' } as RouterStateSnapshot),
    );

  it('authGuard_whenAnonymous_thenRedirectsToLoginKeepingReturnUrl', () => {
    const result = run();

    expect(result).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Fdashboard');
  });

  it('authGuard_whenAuthenticated_thenAllowsNavigation', () => {
    auth.login('ana', 's3cret-pass').subscribe();
    http.expectOne('/api/auth/login').flush({ accessToken: 'a' });
    http.expectOne('/api/auth/me').flush({ username: 'ana', roles: ['USER'] });

    expect(run()).toBe(true);
  });
});
