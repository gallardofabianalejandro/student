import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { errorPageInterceptor } from './error-page.interceptor';

describe('errorPageInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([errorPageInterceptor])), provideHttpClientTesting()],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  function fail(method: 'GET' | 'POST', url: string, status: number): HttpErrorResponse | undefined {
    let received: HttpErrorResponse | undefined;
    client.request(method, url, { body: method === 'POST' ? {} : undefined }).subscribe({
      error: (e: HttpErrorResponse) => (received = e),
    });
    http.expectOne(url).flush({}, { status, statusText: 'x' });
    return received;
  }

  it('intercept_whenApiAnswers403_thenGoesToForbiddenPageAndKeepsTheError', () => {
    const error = fail('GET', '/api/v1/sample-items', 403);

    expect(navigate).toHaveBeenCalledWith('/forbidden', { replaceUrl: true });
    expect(error?.status).toBe(403);
  });

  it('intercept_whenAMutationIsForbidden_thenAlsoGoesToForbiddenPage', () => {
    fail('POST', '/api/v1/sample-items', 403);

    expect(navigate).toHaveBeenCalledWith('/forbidden', { replaceUrl: true });
  });

  it('intercept_whenALoadFailsWith500_thenReplacesTheHistoryEntryAndRemembersWhereItCameFrom', () => {
    fail('GET', '/api/v1/sample-items', 500);

    expect(navigate).toHaveBeenCalledWith('/error', { replaceUrl: true, state: { from: '/' } });
  });

  it('intercept_whenALoadFailsWith503_thenGoesToErrorPage', () => {
    fail('GET', '/api/v1/sample-items', 503);

    expect(navigate).toHaveBeenCalledWith('/error', { replaceUrl: true, state: { from: '/' } });
  });

  it('intercept_whenAMutationFailsWith500_thenStaysOnThePageSoTheFormIsNotLost', () => {
    fail('POST', '/api/v1/sample-items', 500);

    expect(navigate).not.toHaveBeenCalled();
  });

  it.each([404, 409, 422])('intercept_whenApiAnswers%i_thenLeavesItToTheCaller', (status) => {
    fail('GET', '/api/v1/sample-items', status);

    expect(navigate).not.toHaveBeenCalled();
  });

  it.each(['/api/auth/login', '/api/auth/register', '/api/auth/me'])(
    'intercept_whenAuthEndpoint%sFailsWith500_thenLeavesItToTheForm',
    (url) => {
      fail('GET', url, 500);

      expect(navigate).not.toHaveBeenCalled();
    },
  );

  it('intercept_whenAnotherOriginFails_thenIgnoresIt', () => {
    fail('GET', 'https://otro.example/datos', 500);

    expect(navigate).not.toHaveBeenCalled();
  });
});
