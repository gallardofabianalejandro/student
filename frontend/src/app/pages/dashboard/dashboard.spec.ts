import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {AuthService} from '../../services/auth.service';
import {Dashboard} from './dashboard';

describe('Dashboard', () => {
  function render(user: { username: string; roles: string[] } | null): string {
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideRouter([]), {provide: AuthService, useValue: {user: () => user}}],
    });
    const fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('render_whenUserHasRoles_thenShowsUsernameAndEveryRole', () => {
    const content = render({ username: 'ana', roles: ['ADMIN', 'USER'] });

    expect(content).toContain('ana');
    expect(content).toContain('ADMIN');
    expect(content).toContain('USER');
  });

  it('render_whenRendered_thenOffersAShortcutToItems', () => {
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideRouter([]), {provide: AuthService, useValue: {user: () => null}}],
    });
    const fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();

    const link = (fixture.nativeElement as HTMLElement).querySelector('a[href="/items"]');

    expect(link?.textContent).toContain('Items');
  });

  it('render_whenNoUser_thenShowsTitleWithoutUserData', () => {
    const content = render(null);

    expect(content).toContain('Dashboard');
    expect(content).not.toContain('Sesión iniciada');
  });
});
