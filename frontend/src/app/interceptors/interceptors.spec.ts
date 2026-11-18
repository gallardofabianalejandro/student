import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';
import { refreshTokenInterceptor } from './refresh-token.interceptor';

describe('HTTP interceptors', () => {
  let client: HttpClient;
  let http: HttpTestingController;
  let auth: AuthService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([refreshTokenInterceptor, authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
  });

  afterEach(() => http.verify());

  function signIn(token: string): void {
    auth.login('ana', 's3cret-pass').subscribe();
    http.expectOne('/api/auth/login').flush({ accessToken: token });
    http.expectOne('/api/auth/me').flush({ username: 'ana', roles: ['USER'] });
  }

  it('authInterceptor_whenSignedIn_thenAddsBearerToApiRequests', () => {
    signIn('access-1');

    client.get('/api/items').subscribe();

    expect(http.expectOne('/api/items').request.headers.get('Authorization')).toBe('Bearer access-1');
  });

  it('authInterceptor_whenRequestGoesToAnotherOrigin_thenNeverLeaksToken', () => {
    signIn('access-1');

    client.get('https://example.com/api/items').subscribe();

    expect(http.expectOne('https://example.com/api/items').request.headers.has('Authorization')).toBe(false);
  });

  it('authInterceptor_whenAnonymous_thenNoAuthorizationHeader', () => {
    client.get('/api/items').subscribe();

    expect(http.expectOne('/api/items').request.headers.has('Authorization')).toBe(false);
  });

  it('refreshInterceptor_when401_thenRefreshesAndRetriesWithNewToken', () => {
    signIn('old');
    let body: unknown;
    client.get('/api/items').subscribe((b) => (body = b));

    http.expectOne('/api/items').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/auth/refresh').flush({ accessToken: 'new' });
    const retry = http.expectOne('/api/items');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer new');
    retry.flush({ ok: true });

    expect(body).toEqual({ ok: true });
  });

  it('refreshInterceptor_whenRefreshFails_thenLogsOutAndRedirectsToLogin', () => {
    signIn('old');
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    let failed = false;
    client.get('/api/items').subscribe({ error: () => (failed = true) });

    http.expectOne('/api/items').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(failed).toBe(true);
    expect(auth.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login'], expect.anything());
  });

  it('refreshInterceptor_whenLoginItselfReturns401_thenDoesNotTryToRefresh', () => {
    let failed = false;
    client.post('/api/auth/login', {}).subscribe({ error: () => (failed = true) });

    http.expectOne('/api/auth/login').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(failed).toBe(true);
    http.expectNone('/api/auth/refresh');
  });

  it('refreshInterceptor_whenErrorIsNot401_thenPropagatesWithoutRefreshing', () => {
    let status = 0;
    client.get('/api/items').subscribe({ error: (e) => (status = e.status) });

    http.expectOne('/api/items').flush({}, { status: 500, statusText: 'Server Error' });

    expect(status).toBe(500);
    http.expectNone('/api/auth/refresh');
  });
});
