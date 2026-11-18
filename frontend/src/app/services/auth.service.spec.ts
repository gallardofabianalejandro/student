import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function completeLogin(): void {
    http.expectOne('/api/auth/login').flush({ accessToken: 'access-1' });
    http.expectOne('/api/auth/me').flush({ username: 'ana', roles: ['USER'] });
  }

  it('login_whenCredentialsValid_thenKeepsTokenInMemoryAndLoadsUser', () => {
    service.login('ana', 's3cret-pass').subscribe();

    const login = http.expectOne('/api/auth/login');
    expect(login.request.body).toEqual({ username: 'ana', password: 's3cret-pass' });
    login.flush({ accessToken: 'access-1' });
    http.expectOne('/api/auth/me').flush({ username: 'ana', roles: ['USER'] });

    expect(service.getAccessToken()).toBe('access-1');
    expect(service.user()).toEqual({ username: 'ana', roles: ['USER'] });
    expect(service.isAuthenticated()).toBe(true);
  });

  it('login_whenSucceeds_thenNothingIsPersistedInBrowserStorage', () => {
    service.login('ana', 's3cret-pass').subscribe();
    completeLogin();

    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });

  it('login_whenCredentialsInvalid_thenStaysAnonymous', () => {
    let failed = false;
    service.login('ana', 'bad').subscribe({ error: () => (failed = true) });

    http.expectOne('/api/auth/login').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(failed).toBe(true);
    expect(service.isAuthenticated()).toBe(false);
    expect(service.getAccessToken()).toBeNull();
  });

  it('refreshAccessToken_whenCalled_thenSendsCookieAndCsrfHeaderAndStoresNewToken', () => {
    service.refreshAccessToken().subscribe();

    const req = http.expectOne('/api/auth/refresh');
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);
    expect(req.request.headers.get('X-Requested-With')).toBe('XMLHttpRequest');
    req.flush({ accessToken: 'access-2' });

    expect(service.getAccessToken()).toBe('access-2');
  });

  it('refreshAccessToken_whenCalledConcurrently_thenSingleRequest', () => {
    service.refreshAccessToken().subscribe();
    service.refreshAccessToken().subscribe();

    http.expectOne('/api/auth/refresh').flush({ accessToken: 'access-2' });
  });

  it('restoreSession_whenRefreshCookieValid_thenUserIsRestored', async () => {
    const restored = service.restoreSession();

    http.expectOne('/api/auth/refresh').flush({ accessToken: 'access-3' });
    await Promise.resolve();
    http.expectOne('/api/auth/me').flush({ username: 'ana', roles: ['ADMIN'] });
    await restored;

    expect(service.user()?.username).toBe('ana');
    expect(service.getAccessToken()).toBe('access-3');
  });

  it('restoreSession_whenNoRefreshCookie_thenResolvesAnonymousWithoutThrowing', async () => {
    const restored = service.restoreSession();

    http.expectOne('/api/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });
    await restored;

    expect(service.isAuthenticated()).toBe(false);
  });

  it('logout_whenAuthenticated_thenClearsStateEvenIfServerFails', () => {
    service.login('ana', 's3cret-pass').subscribe();
    completeLogin();

    service.logout().subscribe();
    http.expectOne('/api/auth/logout').flush({}, { status: 500, statusText: 'Server Error' });

    expect(service.isAuthenticated()).toBe(false);
    expect(service.getAccessToken()).toBeNull();
  });

  it('register_whenRegistrationDisabled_thenPropagatesNotFound', () => {
    let status = 0;
    service.register('ana', 's3cret-pass').subscribe({ error: (e) => (status = e.status) });

    http.expectOne('/api/auth/register').flush({}, { status: 404, statusText: 'Not Found' });

    expect(status).toBe(404);
  });
});
