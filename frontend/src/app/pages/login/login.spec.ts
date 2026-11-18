import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { Login } from './login';

describe('Login', () => {
  const login = vi.fn();
  let router: Router;

  function setup(returnUrl: string | null = null) {
    login.mockReset();
    TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { login } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(returnUrl ? { returnUrl } : {}) } },
        },
      ],
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    return fixture;
  }

  function fill(fixture: ReturnType<typeof setup>, username: string, password: string): void {
    fixture.componentInstance['form'].setValue({ username, password });
  }

  function alertText(fixture: ReturnType<typeof setup>): string {
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent?.trim() ?? '';
  }

  it('submit_whenCredentialsValid_thenCallsLoginAndGoesToDashboard', () => {
    const fixture = setup();
    login.mockReturnValue(of({ username: 'ana', roles: ['USER'] }));
    fill(fixture, 'ana', 's3cret-pass');

    fixture.componentInstance.submit();

    expect(login).toHaveBeenCalledWith('ana', 's3cret-pass');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('submit_whenInternalReturnUrl_thenGoesBackThere', () => {
    const fixture = setup('/dashboard/profile');
    login.mockReturnValue(of({ username: 'ana', roles: ['USER'] }));
    fill(fixture, 'ana', 's3cret-pass');

    fixture.componentInstance.submit();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard/profile');
  });

  it.each(['https://evil.example/phish', '//evil.example', 'javascript:alert(1)'])(
    'submit_whenReturnUrlIsExternal_thenIgnoresItAndGoesToDashboard(%s)',
    (returnUrl) => {
      const fixture = setup(returnUrl);
      login.mockReturnValue(of({ username: 'ana', roles: ['USER'] }));
      fill(fixture, 'ana', 's3cret-pass');

      fixture.componentInstance.submit();

      expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
    },
  );

  it('submit_whenServerReturns401_thenShowsWrongCredentialsMessage', () => {
    const fixture = setup();
    login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
    fill(fixture, 'ana', 'bad');

    fixture.componentInstance.submit();

    expect(alertText(fixture)).toContain('incorrectos');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('submit_whenServerFails_thenShowsGenericMessageAndAllowsRetry', () => {
    const fixture = setup();
    login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    fill(fixture, 'ana', 's3cret-pass');

    fixture.componentInstance.submit();

    expect(alertText(fixture)).toContain('No se pudo iniciar sesión');
    expect(fixture.componentInstance['loading']()).toBe(false);
  });

  it('submit_whenFormEmpty_thenDoesNotCallServer', () => {
    const fixture = setup();

    fixture.componentInstance.submit();

    expect(login).not.toHaveBeenCalled();
  });
});
