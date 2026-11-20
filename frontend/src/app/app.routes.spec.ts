import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';

describe('routes', () => {
  async function visit(url: string): Promise<HTMLElement> {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url);
    return harness.routeNativeElement as HTMLElement;
  }

  it('navigate_whenUnknownPath_thenShowsNotFoundInsteadOfRedirecting', async () => {
    const page = await visit('/no-existe');

    expect(page.textContent).toContain('404');
  });

  it('navigate_whenForbiddenPath_thenShowsForbiddenPage', async () => {
    const page = await visit('/forbidden');

    expect(page.textContent).toContain('403');
  });

  it('routes_whenDeclared_thenEveryPageHasATitle', () => {
    const untitled = routes.filter((r) => r.path !== '**' && !r.title && !r.redirectTo).map((r) => r.path);

    expect(untitled).toEqual([]);
  });

  it('navigate_whenErrorPath_thenShowsServerErrorPage', async () => {
    const page = await visit('/error');

    expect(page.textContent).toContain('500');
  });
});
