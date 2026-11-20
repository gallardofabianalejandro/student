import { Location } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ServerError } from './server-error';

describe('ServerError', () => {
  function render(state: unknown = null): HTMLElement {
    TestBed.configureTestingModule({ imports: [ServerError], providers: [provideRouter([])] });
    vi.spyOn(TestBed.inject(Location), 'getState').mockReturnValue(state);
    const fixture = TestBed.createComponent(ServerError);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  const retryButton = (page: HTMLElement) =>
    Array.from(page.querySelectorAll('button')).find((b) => b.textContent?.includes('Reintentar'));

  it('render_whenCreated_thenShowsTheCodeAndAHeading', () => {
    const page = render();

    expect(page.textContent).toContain('500');
    expect(page.querySelector('h1')?.textContent).toContain('Algo salió mal');
  });

  it('retry_whenWeKnowWhereWeCameFrom_thenLoadsThatPageAgain', () => {
    const page = render({ from: '/items' });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    retryButton(page)?.click();

    expect(navigate).toHaveBeenCalledWith('/items');
  });

  it('retry_whenTheOriginIsUnknown_thenGoesBackInTheHistory', () => {
    const page = render(null);
    const back = vi.spyOn(TestBed.inject(Location), 'back').mockImplementation(() => undefined);

    retryButton(page)?.click();

    expect(back).toHaveBeenCalledOnce();
  });

  it.each(['https://evil.example', '//evil.example', 'javascript:alert(1)'])(
    'retry_whenTheStoredOriginIsNotAnInternalPath_thenIgnoresIt(%s)',
    (from) => {
      const page = render({ from });
      const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
      const back = vi.spyOn(TestBed.inject(Location), 'back').mockImplementation(() => undefined);

      retryButton(page)?.click();

      expect(navigate).not.toHaveBeenCalled();
      expect(back).toHaveBeenCalledOnce();
    },
  );

  it('render_whenCreated_thenOffersAWayBackHome', () => {
    expect(render().querySelector('a[href="/"]')?.textContent).toContain('Volver al inicio');
  });
});
