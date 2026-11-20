import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { Navbar } from './navbar';

describe('Navbar', () => {
  const logout = vi.fn();

  function setup(user: { username: string; roles: string[] } | null) {
    logout.mockReset();
    logout.mockReturnValue(of(undefined));
    TestBed.configureTestingModule({
      imports: [Navbar],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { isAuthenticated: () => user !== null, user: () => user, logout } },
      ],
    });
    const fixture = TestBed.createComponent(Navbar);
    fixture.detectChanges();
    return fixture;
  }

  const text = (f: ReturnType<typeof setup>) => (f.nativeElement as HTMLElement).textContent ?? '';
  const buttons = (f: ReturnType<typeof setup>) =>
    Array.from((f.nativeElement as HTMLElement).querySelectorAll('button'));

  it('render_whenAnonymous_thenOffersLoginAndRegisterOnly', () => {
    const fixture = setup(null);

    expect(text(fixture)).toContain('Ingresar');
    expect(text(fixture)).toContain('Registrarse');
    expect(text(fixture)).not.toContain('Salir');
    expect(text(fixture)).not.toContain('Dashboard');
  });

  it('render_whenAuthenticated_thenShowsUsernameDashboardAndLogout', () => {
    const fixture = setup({ username: 'ana', roles: ['USER'] });

    expect(text(fixture)).toContain('ana');
    expect(text(fixture)).toContain('Dashboard');
    expect(text(fixture)).toContain('Salir');
    expect(text(fixture)).not.toContain('Registrarse');
  });

  it('logout_whenClickingExit_thenCallsServiceAndGoesHome', () => {
    const fixture = setup({ username: 'ana', roles: ['USER'] });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    const button = buttons(fixture).find((b) => b.textContent?.includes('Salir')) as HTMLButtonElement;
    button.click();

    expect(logout).toHaveBeenCalledOnce();
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('themeToggle_whenAnonymous_thenIsAvailableAndSwitchesTheTheme', () => {
    localStorage.setItem('theme', 'light');
    const fixture = setup(null);
    const toggle = buttons(fixture).find((b) => b.getAttribute('aria-label') === 'Cambiar tema') as HTMLButtonElement;

    toggle.click();

    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('themeToggle_whenAuthenticated_thenIsAvailableToo', () => {
    const fixture = setup({ username: 'ana', roles: ['USER'] });

    expect(buttons(fixture).some((b) => b.getAttribute('aria-label') === 'Cambiar tema')).toBe(true);
  });
});
